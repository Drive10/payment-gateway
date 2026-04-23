package dev.payment.paymentservice.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.payment.domain.IdempotencyRecord;
import dev.payment.paymentservice.payment.domain.enums.IdempotencyRecordStatus;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.repository.IdempotencyRecordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String REDIS_KEY_PREFIX = "payflow:idempotency:";
    
    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Duration redisTtl;
    private final Counter redisHitCounter;
    private final Counter redisMissCounter;

    public IdempotencyService(
            IdempotencyRecordRepository repository,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            @Value("${application.idempotency.redis-ttl-hours:24}") long redisTtlHours,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.redisTtl = Duration.ofHours(redisTtlHours);
        
        this.redisHitCounter = Counter.builder("idempotency.redis.hit")
                .description("Redis cache hits for idempotency")
                .register(meterRegistry);
        this.redisMissCounter = Counter.builder("idempotency.redis.miss")
                .description("Redis cache misses for idempotency")
                .register(meterRegistry);
    }

    @Transactional
    public <T> IdempotencyResult<T> begin(String operation, String idempotencyKey, Long actorId, Object requestFingerprint, Class<T> responseType) {
        String requestHash = hash(requestFingerprint);
        String cacheKey = buildCacheKey(operation, actorId, idempotencyKey);

        Optional<IdempotencyRecord> cachedResult = checkRedisCache(cacheKey, requestHash, responseType);
        if (cachedResult.isPresent()) {
            redisHitCounter.increment();
            return resolveExisting(cachedResult.get(), requestHash, responseType);
        }
        
        redisMissCounter.increment();

        Optional<IdempotencyRecord> existing = repository.findByOperationAndActorIdAndIdempotencyKey(operation, actorId, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            cacheToRedis(cacheKey, record);
            return resolveExisting(record, requestHash, responseType);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setOperation(operation);
        record.setActorId(actorId);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyRecordStatus.IN_PROGRESS);

        try {
            IdempotencyRecord saved = repository.save(record);
            return IdempotencyResult.started(saved, null);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyRecord concurrent = repository.findByOperationAndActorIdAndIdempotencyKey(operation, actorId, idempotencyKey)
                    .orElseThrow(() -> exception);
            cacheToRedis(cacheKey, concurrent);
            return resolveExisting(concurrent, requestHash, responseType);
        }
    }

    @Transactional
    public void complete(IdempotencyRecord record, Object responseBody, String resourceId) {
        try {
            record.setStatus(IdempotencyRecordStatus.COMPLETED);
            record.setResourceId(resourceId);
            record.setResponsePayload(objectMapper.writeValueAsString(responseBody));
            repository.save(record);
            
            String cacheKey = buildCacheKey(record.getOperation(), record.getActorId(), record.getIdempotencyKey());
            cacheToRedis(cacheKey, record);
            
            log.debug("Idempotency record completed: operation={}, key={}", record.getOperation(), record.getIdempotencyKey());
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_STORE_FAILED", "Unable to persist idempotent response");
        }
    }

    private <T> Optional<IdempotencyRecord> checkRedisCache(String cacheKey, String requestHash, Class<T> responseType) {
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                IdempotencyRecord record = objectMapper.readValue(cached, IdempotencyRecord.class);
                if (record.getRequestHash().equals(requestHash)) {
                    log.debug("Redis cache hit for idempotency key: {}", cacheKey);
                    return Optional.of(record);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check Redis cache for idempotency key: {}", cacheKey, e);
        }
        return Optional.empty();
    }

    private void cacheToRedis(String cacheKey, IdempotencyRecord record) {
        try {
            String value = objectMapper.writeValueAsString(record);
            redisTemplate.opsForValue().set(cacheKey, value, redisTtl);
            log.debug("Cached idempotency record to Redis: key={}, ttl={}", cacheKey, redisTtl);
        } catch (Exception e) {
            log.warn("Failed to cache idempotency record to Redis: key={}", cacheKey, e);
        }
    }

    private String buildCacheKey(String operation, Long actorId, String idempotencyKey) {
        return REDIS_KEY_PREFIX + operation + ":" + actorId + ":" + idempotencyKey;
    }

    private <T> IdempotencyResult<T> resolveExisting(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED", "Idempotency key cannot be reused with a different request");
        }

        if (existing.getStatus() == IdempotencyRecordStatus.IN_PROGRESS) {
            throw new ApiException(HttpStatus.CONFLICT, "REQUEST_ALREADY_IN_PROGRESS", "A matching request with this idempotency key is still processing");
        }

        try {
            T response = objectMapper.readValue(existing.getResponsePayload(), responseType);
            return IdempotencyResult.replayed(existing, response);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_REPLAY_FAILED", "Unable to replay the stored idempotent response");
        }
    }

    private String hash(Object value) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(value);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_HASH_FAILED", "Unable to fingerprint idempotent request");
        }
    }

    public record IdempotencyResult<T>(IdempotencyRecord record, T cachedResponse, boolean replayed) {
        public static <T> IdempotencyResult<T> started(IdempotencyRecord record, T cachedResponse) {
            return new IdempotencyResult<>(record, cachedResponse, false);
        }

        public static <T> IdempotencyResult<T> replayed(IdempotencyRecord record, T cachedResponse) {
            return new IdempotencyResult<>(record, cachedResponse, true);
        }
    }
}
