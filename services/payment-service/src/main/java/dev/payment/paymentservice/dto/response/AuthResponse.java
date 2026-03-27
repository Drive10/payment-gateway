package dev.payment.paymentservice.dto.response;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        long refreshExpiresInSeconds,
        String email,
        Set<String> roles
) {
}
