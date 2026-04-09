package dev.payment.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record KycRejectionRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
