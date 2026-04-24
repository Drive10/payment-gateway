package dev.payment.notificationservice.infrastructure.persistence.repository;

import dev.payment.notificationservice.domain.entities.WebhookEvent;
import dev.payment.notificationservice.domain.entities.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    List<WebhookEvent> findByStatus(WebhookStatus status);
    boolean existsByEventTypeAndProvider(String eventType, String provider);
    
    Optional<WebhookEvent> findByProviderAndEventId(String provider, String eventId);
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.status = :status AND w.nextRetryAt <= :now AND w.attempt < :maxAttempts ORDER BY w.createdAt ASC")
    List<WebhookEvent> findPendingRetries(@Param("now") Instant now, @Param("maxAttempts") int maxAttempts);
}
