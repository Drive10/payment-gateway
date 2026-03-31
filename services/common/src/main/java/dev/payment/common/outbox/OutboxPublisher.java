package dev.payment.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public <T> OutboxEvent publish(String aggregateType, String aggregateId, String eventType, T payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            
            OutboxEvent event = new OutboxEvent();
            event.setId(UUID.randomUUID().toString());
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setPayload(payloadJson);
            event.setCreatedAt(Instant.now());
            event.setScheduledAt(Instant.now());
            event.setStatus(OutboxEvent.OutboxStatus.PENDING);
            event.setRetryCount(0);
            event.setVersion(1);

            OutboxEvent saved = repository.save(event);
            log.info("Published outbox event: type={}, aggregateId={}, eventId={}", 
                    eventType, aggregateId, saved.getId());
            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for outbox event: {}", e.getMessage());
            throw new RuntimeException("Failed to publish outbox event", e);
        }
    }

    public <T> OutboxEvent publishWithDelay(String aggregateType, String aggregateId, 
            String eventType, T payload, long delaySeconds) {
        OutboxEvent event = publish(aggregateType, aggregateId, eventType, payload);
        event.setScheduledAt(Instant.now().plusSeconds(delaySeconds));
        return repository.save(event);
    }
}
