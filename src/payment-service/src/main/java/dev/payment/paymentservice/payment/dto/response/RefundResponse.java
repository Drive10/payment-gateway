package dev.payment.paymentservice.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        String refundReference,
        UUID paymentId,
        String orderReference,
        BigDecimal amount,
        BigDecimal refundedAmount,
        String paymentStatus,
        String reason,
        Instant processedAt
) {
}
