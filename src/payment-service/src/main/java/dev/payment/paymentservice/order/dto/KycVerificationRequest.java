package dev.payment.paymentservice.order.dto;

import jakarta.validation.constraints.NotBlank;

public record KycVerificationRequest(
        @NotBlank(message = "Notes are required")
        String notes
) {
}
