package dev.payment.merchantbackend.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    Long expiresIn
) {}