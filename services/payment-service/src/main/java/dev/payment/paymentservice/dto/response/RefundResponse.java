package dev.payment.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID paymentId,
        String orderReference,
        BigDecimal refundedAmount,
        String paymentStatus,
        String reason,
        Instant processedAt
) {
}
