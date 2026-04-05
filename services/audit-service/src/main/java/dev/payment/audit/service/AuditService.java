package dev.payment.audit.service;

import dev.payment.audit.document.AuditLog;
import dev.payment.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public CompletableFuture<AuditLog> logAsync(AuditLog auditLog) {
        return CompletableFuture.completedFuture(auditLogRepository.save(auditLog));
    }

    public AuditLog log(AuditLog auditLog) {
        if (auditLog.getTimestamp() == null) {
            auditLog.setTimestamp(Instant.now());
        }
        return auditLogRepository.save(auditLog);
    }

    public AuditLog createLog(String userId, String entityType, String entityId, 
                              String action, String service, boolean success) {
        return log(AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .service(service)
                .success(success)
                .timestamp(Instant.now())
                .build());
    }

    public AuditLog createLog(String userId, String entityType, String entityId,
                              String action, String service, boolean success,
                              Map<String, Object> oldValue, Map<String, Object> newValue) {
        return log(AuditLog.builder()
                .userId(userId)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .service(service)
                .success(success)
                .oldValue(oldValue)
                .newValue(newValue)
                .timestamp(Instant.now())
                .build());
    }

    public Page<AuditLog> getLogsByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    public Page<AuditLog> getLogsByEntity(String entityType, String entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    public Page<AuditLog> getLogsByService(String service, Pageable pageable) {
        return auditLogRepository.findByService(service, pageable);
    }

    public List<AuditLog> getLogsBetween(Instant start, Instant end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    public Page<AuditLog> searchLogs(String action, Instant start, Instant end, Pageable pageable) {
        return auditLogRepository.findByActionAndTimestampBetween(action, start, end, pageable);
    }
}
