package dev.payment.combinedservice.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderReference,
        String externalReference,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        Instant createdAt
) {
}
