package dev.payment.notificationservice.dto;

import dev.payment.notificationservice.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;

public record CreateTemplateRequest(
        @NotBlank(message = "Template key is required")
        String templateKey,

        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotBlank(message = "Channel is required")
        NotificationChannel channel,

        String subject,

        @NotBlank(message = "Body template is required")
        String bodyTemplate,

        String eventType
) {
}
