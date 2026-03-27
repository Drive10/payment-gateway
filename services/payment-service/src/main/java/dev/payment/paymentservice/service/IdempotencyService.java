package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.domain.IdempotencyRecord;
import dev.payment.paymentservice.domain.enums.IdempotencyRecordStatus;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> IdempotencyResult<T> begin(String operation, String idempotencyKey, Long actorId, Object requestFingerprint, Class<T> responseType) {
        String requestHash = hash(requestFingerprint);
        Optional<IdempotencyRecord> existing = repository.findByOperationAndActorIdAndIdempotencyKey(operation, actorId, idempotencyKey);
        if (existing.isPresent()) {
            return resolveExisting(existing.get(), requestHash, responseType);
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setOperation(operation);
        record.setActorId(actorId);
        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyRecordStatus.IN_PROGRESS);

        try {
            return IdempotencyResult.started(repository.save(record), null);
        } catch (DataIntegrityViolationException exception) {
            IdempotencyRecord concurrent = repository.findByOperationAndActorIdAndIdempotencyKey(operation, actorId, idempotencyKey)
                    .orElseThrow(() -> exception);
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
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_STORE_FAILED", "Unable to persist idempotent response");
        }
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
