package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.events.PaymentEventMessage;
import dev.payment.paymentservice.domain.PaymentOutboxEvent;
import dev.payment.paymentservice.domain.enums.OutboxEventStatus;
import dev.payment.paymentservice.repository.PaymentOutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("!test")
public class PaymentOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(PaymentOutboxRelay.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final String LOCK_KEY = "payflow:outbox:relay:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final PaymentOutboxEventRepository repository;
    private final KafkaTemplate<String, PaymentEventMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Timer relayTimer;
    private final Counter relaySuccessCounter;
    private final Counter relayFailureCounter;
    private final Counter relayDeadLetterCounter;

    public PaymentOutboxRelay(
            PaymentOutboxEventRepository repository,
            KafkaTemplate<String, PaymentEventMessage> kafkaTemplate,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            MeterRegistry meterRegistry
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        
        this.relayTimer = Timer.builder("payment.outbox.relay.duration")
                .description("Time taken to relay outbox events to Kafka")
                .register(meterRegistry);
        this.relaySuccessCounter = Counter.builder("payment.outbox.relay.success")
                .description("Successfully relayed outbox events")
                .register(meterRegistry);
        this.relayFailureCounter = Counter.builder("payment.outbox.relay.failure")
                .description("Failed outbox event relays")
                .register(meterRegistry);
        this.relayDeadLetterCounter = Counter.builder("payment.outbox.dead_letter")
                .description("Outbox events moved to dead letter")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${application.outbox.relay-delay-ms:1000}")
    @Transactional
    public void relayPendingEvents() {
        if (!acquireLock()) {
            log.debug("Outbox relay lock not acquired, another instance is processing");
            return;
        }

        try {
            relayTimer.record(() -> {
                List<PaymentOutboxEvent> events = repository.findTop50ByStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
                        OutboxEventStatus.PENDING,
                        Instant.now()
                );

                for (PaymentOutboxEvent event : events) {
                    try {
                        relay(event);
                        relaySuccessCounter.increment();
                    } catch (Exception e) {
                        relayFailureCounter.increment();
                        log.error("Failed to relay outbox event: id={}, eventType={}", 
                                event.getId(), event.getEventType(), e);
                    }
                }
            });
        } finally {
            releaseLock();
        }
    }

    private boolean acquireLock() {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_KEY, UUID.randomUUID().toString(), LOCK_TTL);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Failed to acquire outbox relay lock, proceeding without lock", e);
            return true;
        }
    }

    private void releaseLock() {
        try {
            redisTemplate.delete(LOCK_KEY);
        } catch (Exception e) {
            log.warn("Failed to release outbox relay lock", e);
        }
    }

    private void relay(PaymentOutboxEvent event) {
        String correlationId = event.getCorrelationId();
        MDC.put("correlationId", correlationId != null ? correlationId : event.getId().toString());
        
        try {
            PaymentEventMessage message = objectMapper.readValue(event.getPayload(), PaymentEventMessage.class);
            ProducerRecord<String, PaymentEventMessage> record = new ProducerRecord<>(event.getTopicName(), event.getEventKey(), message);
            
            Map<String, String> headers = event.getMessageHeaders() == null || event.getMessageHeaders().isBlank()
                    ? java.util.Collections.emptyMap()
                    : objectMapper.readValue(event.getMessageHeaders(), objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            headers.forEach((key, value) -> record.headers().add(key, value.getBytes(StandardCharsets.UTF_8)));
            
            if (correlationId != null) {
                record.headers().add("correlation-id", correlationId.getBytes(StandardCharsets.UTF_8));
            }
            
            kafkaTemplate.send(record).get(30, java.util.concurrent.TimeUnit.SECONDS);
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            event.setLastError(null);
            
            log.info("event=outbox_relayed outboxId={} eventType={} topic={} correlationId={}",
                    event.getId(), event.getEventType(), event.getTopicName(), correlationId);
        } catch (java.util.concurrent.TimeoutException e) {
            Thread.currentThread().interrupt();
            handleFailure(event, "Kafka send timeout after 30s");
        } catch (java.util.concurrent.ExecutionException e) {
            Thread.currentThread().interrupt();
            handleFailure(event, e.getCause() != null ? e.getCause().getMessage() : "Kafka execution error");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleFailure(event, "Interrupted while sending to Kafka");
        } catch (Exception exception) {
            handleFailure(event, exception.getMessage());
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void handleFailure(PaymentOutboxEvent event, String reason) {
        int attempts = event.getAttemptCount() + 1;
        event.setAttemptCount(attempts);
        event.setLastError(truncate(reason));
        
        long backoffSeconds = (long) Math.min(300, Math.pow(2, attempts) * 5);
        event.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
        
        if (attempts >= MAX_ATTEMPTS) {
            event.setStatus(OutboxEventStatus.DEAD_LETTER);
            relayDeadLetterCounter.increment();
            log.error("event=outbox_dead_letter outboxId={} eventType={} alert=OUTBOX_DLQ reason={}",
                    event.getId(), event.getEventType(), truncate(reason));
        } else {
            log.warn("event=outbox_retry outboxId={} eventType={} attempt={}/{} reason={}",
                    event.getId(), event.getEventType(), attempts, MAX_ATTEMPTS, truncate(reason));
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
