package dev.payment.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MerchantAnalyticsResponse(
        BigDecimal totalRevenue,
        BigDecimal totalFees,
        BigDecimal totalRefunds,
        BigDecimal netRevenue,
        long totalPayments,
        long successCount,
        long failedCount,
        double successRate
) {
}
