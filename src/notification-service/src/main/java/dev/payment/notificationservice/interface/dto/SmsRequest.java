package dev.payment.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record SmsRequest(
        @NotBlank(message = "Phone number is required")
        String to,

        @NotBlank(message = "Message is required")
        String message,

        String userId,
        String paymentId,
        String orderId
) {
}
