package dev.payment.orderservice.dto;

import dev.payment.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        String description,
        Instant createdAt
) {
}
