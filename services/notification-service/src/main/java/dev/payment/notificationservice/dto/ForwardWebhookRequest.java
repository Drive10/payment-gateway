package dev.payment.notificationservice.dto;

import java.time.Instant;
import java.util.UUID;

public record ForwardWebhookRequest(
        UUID eventId,
        String eventType,
        Object payload,
        Instant timestamp
) {
}
