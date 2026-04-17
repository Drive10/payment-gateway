package dev.payment.notificationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookPayload(
        String eventType,
        String paymentId,
        String status,
        Map<String, Object> metadata,
        Instant timestamp
) {
}
