package dev.payment.combinedservice.order.dto;

import jakarta.validation.constraints.NotBlank;

public record KycRejectionRequest(
        @NotBlank(message = "Reason is required")
        String reason
) {
}
