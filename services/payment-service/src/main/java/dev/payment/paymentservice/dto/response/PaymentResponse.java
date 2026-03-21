package dev.payment.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String orderReference,
        BigDecimal amount,
        String currency,
        String provider,
        String providerOrderId,
        String providerPaymentId,
        String method,
        String status,
        String checkoutUrl,
        String providerSignature,
        String notes,
        Instant createdAt,
        List<PaymentTransactionResponse> transactions
) {
}
