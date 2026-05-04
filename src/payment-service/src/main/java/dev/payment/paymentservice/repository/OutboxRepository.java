package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
    @Query("SELECT o FROM Outbox o WHERE o.processedAt IS NULL AND o.deadLettered = false AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= CURRENT_TIMESTAMP) ORDER BY o.createdAt ASC")
    List<Outbox> findEventsReadyForProcessing();

    @Query("SELECT o FROM Outbox o WHERE o.deadLettered = true ORDER BY o.deadLetteredAt DESC")
    List<Outbox> findDeadLetteredEvents();

    @Query("SELECT o FROM Outbox o WHERE o.processedAt IS NULL AND o.deadLettered = false ORDER BY o.createdAt ASC")
    List<Outbox> findUnprocessedEvents();
}