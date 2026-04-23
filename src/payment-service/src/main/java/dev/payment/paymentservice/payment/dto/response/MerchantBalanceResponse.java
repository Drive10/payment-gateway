package dev.payment.paymentservice.payment.dto.response;

import java.math.BigDecimal;

public record MerchantBalanceResponse(
        BigDecimal availableBalance,
        BigDecimal pendingBalance,
        BigDecimal totalBalance,
        String currency
) {
}
