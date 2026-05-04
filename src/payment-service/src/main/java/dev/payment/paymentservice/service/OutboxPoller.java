package dev.payment.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.entity.Outbox;
import dev.payment.paymentservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {
    public record EventStatus(long pending, long dlq, long processed) {}
    
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<Outbox> events = outboxRepository.findEventsReadyForProcessing();
        
        if (events.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox events", events.size());

        for (Outbox event : events) {
            processEvent(event);
        }
    }

    private void processEvent(Outbox event) {
        try {
            String topic = "payment." + event.getEventType().toLowerCase();
            kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        markAsProcessed(event.getId());
                    } else {
                        handleFailure(event, ex.getMessage());
                    }
                });
        } catch (Exception e) {
            handleFailure(event, e.getMessage());
        }
    }

    private void handleFailure(Outbox event, String error) {
        log.error("Event {} failed (attempt {}): {}", event.getId(), event.getRetryCount() + 1, error);
        
        if (event.canRetry()) {
            event.setErrorMessage(error);
            event.incrementRetry();
            outboxRepository.save(event);
            log.info("Event {} scheduled for retry at {}", event.getId(), event.getNextRetryAt());
        } else {
            deadLetter(event, "Max retries exceeded: " + error);
        }
    }

    private void deadLetter(Outbox event, String reason) {
        event.setDeadLettered(true);
        event.setDeadLetteredAt(Instant.now());
        event.setDeadLetterReason(reason);
        outboxRepository.save(event);
        log.warn("Event {} moved to DLQ: {}", event.getId(), reason);
    }

    private void markAsProcessed(UUID id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setProcessedAt(Instant.now());
            outboxRepository.save(event);
            log.debug("Event {} processed successfully", id);
        });
    }

    public List<Outbox> getDeadLetteredEvents() {
        return outboxRepository.findDeadLetteredEvents();
    }
    
    public EventStatus getEventStatus() {
        List<Outbox> pending = outboxRepository.findUnprocessedEvents();
        List<Outbox> dlq = outboxRepository.findDeadLetteredEvents();
        return new EventStatus(pending.size(), dlq.size(), dlq.size());
    }

    public void replayEvent(UUID eventId) {
        outboxRepository.findById(eventId).ifPresent(event -> {
            event.setDeadLettered(false);
            event.setDeadLetteredAt(null);
            event.setDeadLetterReason(null);
            event.setRetryCount(0);
            event.setNextRetryAt(Instant.now());
            outboxRepository.save(event);
            log.info("Event {} queued for replay", eventId);
        });
    }

    public void replayAllDeadLettered() {
        List<Outbox> dlq = outboxRepository.findDeadLetteredEvents();
        for (Outbox event : dlq) {
            replayEvent(event.getId());
        }
        log.info("Requeued {} DLQ events for replay", dlq.size());
    }

    public void createEvent(String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Outbox event = Outbox.builder()
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonPayload)
                .createdAt(Instant.now())
                .build();
            outboxRepository.save(event);
            log.debug("Created outbox event: {} / {}", aggregateId, eventType);
        } catch (JsonProcessingException e) {
            log.error("Error serializing outbox payload: {}", e.getMessage());
        }
    }

    private final ObjectMapper objectMapper;
}