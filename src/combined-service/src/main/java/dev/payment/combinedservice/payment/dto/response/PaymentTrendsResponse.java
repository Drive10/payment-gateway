package dev.payment.combinedservice.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentTrendsResponse(
        List<DailyTrend> trends
) {
    public record DailyTrend(
            LocalDate date,
            BigDecimal revenue,
            long count
    ) {
    }
}
