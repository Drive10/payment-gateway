package dev.payment.paymentservice.dto.response;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String action,
        String actor,
        String resourceType,
        String resourceId,
        String summary,
        Instant createdAt
) {
}
