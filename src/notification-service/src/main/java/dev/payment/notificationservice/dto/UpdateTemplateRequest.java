package dev.payment.notificationservice.dto;

public record UpdateTemplateRequest(
        String name,
        String description,
        String subject,
        String bodyTemplate,
        Boolean active
) {
}
