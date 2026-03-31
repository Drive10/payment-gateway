package dev.payment.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository repository;
    private final OutboxEventPublisher publisher;

    public OutboxProcessor(OutboxEventRepository repository, OutboxEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval:1000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = repository.findPendingEventsWithLimit(Instant.now(), BATCH_SIZE);
        
        if (events.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                processEvent(event);
            } catch (Exception e) {
                log.error("Failed to process outbox event: id={}, error={}", event.getId(), e.getMessage());
                handleFailure(event, e.getMessage());
            }
        }
    }

    private void processEvent(OutboxEvent event) {
        event.markAsProcessing();
        repository.save(event);

        publisher.publish(event);

        event.markAsCompleted();
        repository.save(event);

        log.info("Successfully processed outbox event: id={}, type={}", event.getId(), event.getEventType());
    }

    private void handleFailure(OutboxEvent event, String error) {
        event.setLastError(error);
        event.setRetryCount(event.getRetryCount() + 1);

        if (event.getRetryCount() >= MAX_RETRIES) {
            event.setStatus(OutboxEvent.OutboxStatus.DEAD_LETTER);
            log.warn("Outbox event moved to dead letter: id={}, retries={}", event.getId(), event.getRetryCount());
        } else {
            long delaySeconds = (long) Math.pow(2, event.getRetryCount());
            event.setScheduledAt(Instant.now().plusSeconds(delaySeconds));
            event.setStatus(OutboxEvent.OutboxStatus.PENDING);
            log.warn("Outbox event scheduled for retry: id={}, attempt={}, nextRetry={}s", 
                    event.getId(), event.getRetryCount(), delaySeconds);
        }

        repository.save(event);
    }
}
