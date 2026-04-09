package dev.payment.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Invalid email format")
        String to,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Body is required")
        String body,

        String from,
        String replyTo,
        String templateId,
        String userId,
        String paymentId,
        String orderId
) {
}
