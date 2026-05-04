package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.WebhookInboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookInboxEventRepository extends JpaRepository<WebhookInboxEvent, UUID> {
    boolean existsByWebhookId(String webhookId);
    Optional<WebhookInboxEvent> findByWebhookId(String webhookId);
}
