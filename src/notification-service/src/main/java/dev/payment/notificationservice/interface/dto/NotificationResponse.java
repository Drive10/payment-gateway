package dev.payment.notificationservice.dto;

import dev.payment.notificationservice.domain.entities.NotificationChannel;
import dev.payment.notificationservice.domain.entities.NotificationStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        NotificationChannel channel,
        NotificationStatus status,
        String recipient,
        String subject,
        String body,
        String eventType,
        Map<String, Object> metadata,
        int retryCount,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime sentAt
) {
}
