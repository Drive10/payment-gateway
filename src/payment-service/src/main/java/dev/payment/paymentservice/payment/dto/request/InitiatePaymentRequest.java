package dev.payment.paymentservice.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record InitiatePaymentRequest(
    UUID orderId,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency,
    String returnUrl,
    String cardToken,
    String merchantId,
    String idempotencyKey,
    String upiId,
    String bankCode,
    String paymentMethod,
    String email
) {
}
