package dev.payment.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiClientRequest(
        @NotBlank @Size(max = 80) String clientCode,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Size(max = 255) String webhookUrl,
        @NotBlank @Size(max = 255) String scopes
) {
}
