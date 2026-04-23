package dev.payment.combinedservice.order.dto;

import dev.payment.combinedservice.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        String orderReference,
        String externalReference,
        String merchantId,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        String description,
        Instant createdAt,
        String customerEmail
) {
}
