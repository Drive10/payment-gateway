package dev.payment.notificationservice.dto;

import dev.payment.notificationservice.entity.NotificationChannel;

import java.time.LocalDateTime;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String templateKey,
        String name,
        String description,
        NotificationChannel channel,
        String subject,
        String bodyTemplate,
        String eventType,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
