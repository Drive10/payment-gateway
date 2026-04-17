package dev.payment.paymentservice.dto.response;

import java.math.BigDecimal;

public record MerchantBalanceResponse(
        BigDecimal availableBalance,
        BigDecimal pendingBalance,
        BigDecimal totalBalance,
        String currency
) {
}
