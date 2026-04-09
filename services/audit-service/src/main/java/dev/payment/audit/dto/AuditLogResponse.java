package dev.payment.audit.dto;

import dev.payment.audit.document.AuditLog;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        String id,
        String userId,
        String entityType,
        String entityId,
        String action,
        String service,
        Instant timestamp,
        Instant createdAt,
        String ipAddress,
        String userAgent,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        boolean success,
        String errorMessage
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getService(),
                log.getTimestamp(),
                log.getCreatedAt(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getOldValue(),
                log.getNewValue(),
                log.isSuccess(),
                log.getErrorMessage()
        );
    }
}
