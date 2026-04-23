package dev.payment.paymentservice.payment.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateFeeConfigRequest(
        @NotNull UUID merchantId,
        String pricingTier,
        @DecimalMin("0") BigDecimal platformFeePercent,
        @DecimalMin("0") BigDecimal platformFixedFee,
        @DecimalMin("0") BigDecimal gatewayFeePercent,
        @DecimalMin("0") BigDecimal gatewayFixedFee,
        BigDecimal volumeThreshold,
        BigDecimal volumeDiscountPercent,
        @DecimalMin("0") BigDecimal minFee,
        @DecimalMin("0") BigDecimal maxFeePercent
) {
}
