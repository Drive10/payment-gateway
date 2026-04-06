package dev.payment.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeeConfigResponse(
        UUID id,
        UUID merchantId,
        String pricingTier,
        BigDecimal platformFeePercent,
        BigDecimal platformFixedFee,
        BigDecimal gatewayFeePercent,
        BigDecimal gatewayFixedFee,
        BigDecimal volumeThreshold,
        BigDecimal volumeDiscountPercent,
        BigDecimal minFee,
        BigDecimal maxFeePercent,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
