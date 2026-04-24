package dev.payment.merchantbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record FrontendPaymentRequest(
    @NotBlank(message = "productId is required")
    String productId
) {}