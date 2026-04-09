package dev.payment.audit.dto;

import java.time.Instant;
import java.util.Map;

public record CreateAuditLogRequest(
        String userId,
        String entityType,
        String entityId,
        String action,
        String service,
        Instant timestamp,
        String ipAddress,
        String userAgent,
        Map<String, Object> oldValue,
        Map<String, Object> newValue,
        boolean success,
        String errorMessage
) {
}
