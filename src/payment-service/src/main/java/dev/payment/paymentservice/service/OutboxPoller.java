package dev.payment.paymentservice.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPoller {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<Outbox> unprocessed = outboxRepository.findUnprocessedEvents();
        
        if (unprocessed.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox events", unprocessed.size());

        for (Outbox event : unprocessed) {
            try {
                String topic = "payment." + event.getEventType().toLowerCase();
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markAsProcessed(event.getId());
                        } else {
                            log.error("Failed to publish event {}: {}", event.getId(), ex.getMessage());
                        }
                    });
            } catch (Exception e) {
                log.error("Error publishing event {}: {}", event.getId(), e.getMessage());
                markAsFailed(event.getId(), e.getMessage());
            }
        }
    }

    private void markAsProcessed(java.util.UUID id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setProcessedAt(Instant.now());
            outboxRepository.save(event);
        });
    }

    private void markAsFailed(java.util.UUID id, String error) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setErrorMessage(error);
            outboxRepository.save(event);
        });
    }
}
