package dev.payment.combinedservice.payment.dto;

import java.math.BigDecimal;

public record FeeCalculation(
        BigDecimal grossAmount,
        BigDecimal platformFee,
        BigDecimal gatewayFee,
        BigDecimal totalFee,
        BigDecimal netAmount,
        BigDecimal platformFeePercent,
        BigDecimal gatewayFeePercent
) {
}
