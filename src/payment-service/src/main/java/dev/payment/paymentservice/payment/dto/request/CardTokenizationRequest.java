package dev.payment.paymentservice.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CardTokenizationRequest(
    @NotBlank(message = "Card number is required")
    @JsonProperty("card_number")
    String cardNumber,
    
    @NotBlank(message = "Expiry month is required")
    @JsonProperty("expiry_month")
    String expiryMonth,
    
    @NotBlank(message = "Expiry year is required")
    @JsonProperty("expiry_year")
    String expiryYear,
    
    @NotBlank(message = "CVV is required")
    String cvv
) {}