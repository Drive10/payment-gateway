package dev.payment.authservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ApiClientResponse(
        UUID id,
        String clientCode,
        String displayName,
        String apiKey,
        String webhookUrl,
        String scopes,
        String status,
        Instant createdAt
) {
}
