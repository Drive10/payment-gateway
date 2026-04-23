package dev.payment.paymentservice.payment.service;

import java.math.BigDecimal;

public record ProviderPaymentSnapshot(
        String providerOrderId,
        String providerPaymentId,
        String status,
        BigDecimal amount,
        String currency,
        boolean simulated
) {
}
