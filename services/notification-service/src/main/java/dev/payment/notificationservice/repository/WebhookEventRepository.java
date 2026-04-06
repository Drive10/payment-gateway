package dev.payment.notificationservice.repository;

import dev.payment.notificationservice.entity.WebhookEvent;
import dev.payment.notificationservice.entity.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    List<WebhookEvent> findByStatus(WebhookStatus status);
    boolean existsByEventTypeAndProvider(String eventType, String provider);
}
