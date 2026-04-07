package dev.payment.simulatorservice.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record SimulationResponse(
        String id,
        String orderReference,
        String paymentReference,
        String provider,
        String providerOrderId,
        String providerPaymentId,
        String status,
        BigDecimal amount,
        String currency,
        String checkoutUrl,
        boolean testMode,
        String notes,
        Instant createdAt
) {
}