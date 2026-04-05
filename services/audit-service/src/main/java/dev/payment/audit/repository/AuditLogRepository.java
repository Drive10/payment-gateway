package dev.payment.audit.repository;

import dev.payment.audit.document.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
    
    Page<AuditLog> findByUserId(String userId, Pageable pageable);
    
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
    
    Page<AuditLog> findByService(String service, Pageable pageable);
    
    List<AuditLog> findByTimestampBetween(Instant start, Instant end);
    
    Page<AuditLog> findByActionAndTimestampBetween(String action, Instant start, Instant end, Pageable pageable);
}
