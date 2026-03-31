package dev.payment.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND e.scheduledAt <= :now ORDER BY e.scheduledAt ASC")
    List<OutboxEvent> findPendingEvents(@Param("now") Instant now);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' AND e.scheduledAt <= :now ORDER BY e.scheduledAt ASC LIMIT :limit")
    List<OutboxEvent> findPendingEventsWithLimit(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'COMPLETED', e.processedAt = :processedAt, e.version = e.version + 1 WHERE e.id = :id AND e.version = :version")
    int markAsCompleted(@Param("id") String id, @Param("processedAt") Instant processedAt, @Param("version") int version);

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'FAILED', e.retryCount = e.retryCount + 1, e.lastError = :error, e.scheduledAt = :nextRetry, e.version = e.version + 1 WHERE e.id = :id AND e.version = :version")
    int markAsFailed(@Param("id") String id, @Param("error") String error, @Param("nextRetry") Instant nextRetry, @Param("version") int version);

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'DEAD_LETTER' ORDER BY e.createdAt DESC")
    List<OutboxEvent> findDeadLetterEvents();

    @Query("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = :status")
    long countByStatus(@Param("status") OutboxEvent.OutboxStatus status);

    @Query("SELECT e FROM OutboxEvent e WHERE e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId ORDER BY e.createdAt DESC")
    List<OutboxEvent> findByAggregate(@Param("aggregateType") String aggregateType, @Param("aggregateId") String aggregateId);
}
