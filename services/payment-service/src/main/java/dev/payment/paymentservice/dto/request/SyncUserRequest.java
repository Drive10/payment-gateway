package dev.payment.paymentservice.dto.request;

import jakarta.validation.constraints.Email;

public record SyncUserRequest(
    @Email(message = "Valid email is required")
    String email
) {}
