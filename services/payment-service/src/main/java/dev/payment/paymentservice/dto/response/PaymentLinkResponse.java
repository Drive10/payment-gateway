package dev.payment.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentLinkResponse(
        String paymentLinkId,
        String paymentLinkUrl,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt,
        Instant expiresAt,
        String description,
        CustomerInfo customer
) {
    public record CustomerInfo(
            String name,
            String email,
            String phone
    ) {}
}
