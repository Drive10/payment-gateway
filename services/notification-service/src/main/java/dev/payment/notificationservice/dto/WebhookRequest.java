package dev.payment.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record WebhookRequest(
        @NotBlank(message = "URL is required")
        String url,

        @NotBlank(message = "Event type is required")
        String eventType,

        Object payload,
        String secret,
        int maxRetries,
        Map<String, String> headers
) {
    public WebhookRequest {
        if (maxRetries == 0) {
            maxRetries = 3;
        }
    }
}
