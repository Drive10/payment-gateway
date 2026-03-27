package dev.payment.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentEventMessage(
        String eventType,
        UUID paymentId,
        UUID orderId,
        String orderReference,
        String provider,
        String paymentStatus,
        String transactionMode,
        boolean simulated,
        BigDecimal amount,
        BigDecimal refundedAmount,
        String currency,
        Instant occurredAt,
        Map<String, String> metadata
) {
}
