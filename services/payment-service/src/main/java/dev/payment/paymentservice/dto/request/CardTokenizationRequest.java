package dev.payment.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardTokenizationRequest(
    @NotBlank(message = "Card number is required")
    String cardNumber,
    
    @NotBlank(message = "Expiry month is required")
    String expiryMonth,
    
    @NotBlank(message = "Expiry year is required")
    String expiryYear,
    
    @NotBlank(message = "CVV is required")
    String cvv
) {}