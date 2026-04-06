package dev.payment.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessorRequest(
        UUID requestId,
        UUID paymentId,
        UUID orderId,
        String orderReference,
        String provider,
        BigDecimal amount,
        String currency,
        String transactionMode,
        String action, // "CREATE_INTENT" or "CAPTURE"
        String notes,
        Instant requestedAt,
        String correlationId
) {
}