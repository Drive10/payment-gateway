package dev.payment.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyClientRequest(
        @NotBlank String apiKey
) {
}
