package dev.payment.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
    @NotBlank String cardToken,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String merchantId,
    @NotBlank String idempotencyKey,
    String upiId,
    String bankCode,
    String paymentMethod,
    String email
) {
}
