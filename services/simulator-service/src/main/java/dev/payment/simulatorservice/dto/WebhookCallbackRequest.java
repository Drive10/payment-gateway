package dev.payment.simulatorservice.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record WebhookCallbackRequest(
        String event,
        UUID transactionId,
        String paymentReference,
        String providerOrderId,
        String providerPaymentId,
        String status,
        BigDecimal amount,
        String currency,
        int attempt,
        String timestamp
) {}