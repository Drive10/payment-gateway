package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {
    Optional<ProcessedWebhookEvent> findByEventId(String eventId);
    boolean existsByEventId(String eventId);
}
