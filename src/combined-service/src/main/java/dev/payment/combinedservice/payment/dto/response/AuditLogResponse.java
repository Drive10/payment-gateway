package dev.payment.combinedservice.payment.dto.response;

import java.time.Instant;

import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String actor,
        String resourceType,
        String resourceId,
        String summary,
        Instant createdAt
) {
}
