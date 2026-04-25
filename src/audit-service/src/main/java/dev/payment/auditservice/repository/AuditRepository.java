package dev.payment.auditservice.repository;

import dev.payment.auditservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByAggregateIdOrderByTimestampAsc(String aggregateId);
}
