package dev.payment.paymentservice.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateApiKeyRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotEmpty(message = "At least one permission is required")
        List<String> permissions,

        Integer rateLimitPerMinute,

        String expiresAt
) {
}
