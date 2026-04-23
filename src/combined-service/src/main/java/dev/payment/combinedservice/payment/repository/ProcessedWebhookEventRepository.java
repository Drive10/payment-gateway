package dev.payment.combinedservice.payment.repository;

import dev.payment.combinedservice.payment.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {
    Optional<ProcessedWebhookEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
