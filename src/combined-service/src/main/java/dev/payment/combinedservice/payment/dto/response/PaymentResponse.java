package dev.payment.combinedservice.payment.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String orderReference,
        BigDecimal amount,
        BigDecimal refundedAmount,
        String currency,
        String provider,
        String providerOrderId,
        String providerPaymentId,
        String method,
        String transactionMode,
        String status,
        String checkoutUrl,
        boolean simulated,
        String providerSignature,
        String notes,
        Instant createdAt,
        List<PaymentTransactionResponse> transactions
) {
}
