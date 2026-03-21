package dev.payment.authservice.dto.response;

public record VerificationResponse(
        boolean valid,
        String clientCode,
        String status,
        String scopes
) {
}
