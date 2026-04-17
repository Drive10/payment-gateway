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
        String providerSignature,
        String status,
        BigDecimal amount,
        String currency,
        String checkoutUrl,
        boolean testMode,
        String notes,
        Instant createdAt,
        Instant processedAt,
        boolean requires3ds,
        String threeDsChallengeUrl,
        String threeDsTransactionId,
        String threeDsStatus,
        String declineCode,
        String declineReason,
        String riskScore,
        String velocityCheck,
        String networkRef,
        int retryCount
) {}