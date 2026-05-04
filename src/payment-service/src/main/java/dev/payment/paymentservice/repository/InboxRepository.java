package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Inbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InboxRepository extends JpaRepository<Inbox, UUID> {
    @Query("SELECT i FROM Inbox i WHERE i.processedAt IS NULL AND i.deadLettered = false AND (i.nextRetryAt IS NULL OR i.nextRetryAt <= CURRENT_TIMESTAMP) ORDER BY i.receivedAt ASC")
    List<Inbox> findEventsReadyForProcessing();

    @Query("SELECT i FROM Inbox i WHERE i.deadLettered = true ORDER BY i.deadLetteredAt DESC")
    List<Inbox> findDeadLetteredEvents();

    @Query("SELECT i FROM Inbox i WHERE i.messageId = :messageId")
    Optional<Inbox> findByMessageId(String messageId);

    boolean existsByMessageIdAndProcessedAtIsNotNull(String messageId);
}