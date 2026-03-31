package dev.payment.simulatorservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SimulationResponse(
        UUID id,
        String orderReference,
        String paymentReference,
        String provider,
        String providerOrderId,
        String providerPaymentId,
        String providerSignature,
        String simulationMode,
        String status,
        BigDecimal amount,
        String currency,
        String checkoutUrl,
        boolean simulated,
        String notes,
        Instant createdAt,
        String webhookCallbackUrl
) {
}
