package dev.payment.simulatorservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WebhookCallbackRequest(
        UUID transactionId,
        String paymentReference,
        String providerOrderId,
        String providerPaymentId,
        String status,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {
}
