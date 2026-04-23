package dev.payment.paymentservice.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentLinkRequest(
        @NotNull UUID merchantId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(max = 3) String currency,
        @Size(max = 255) String description,
        @Size(max = 100) String customerName,
        @Size(max = 255) String customerEmail,
        @Size(max = 20) String customerPhone,
        @Size(max = 500) String successUrl,
        @Size(max = 500) String cancelUrl
) {
}
