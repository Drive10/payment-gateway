package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, UUID> {
    @Query("SELECT o FROM Outbox o WHERE o.processedAt IS NULL ORDER BY o.createdAt ASC")
    List<Outbox> findUnprocessedEvents();
}
