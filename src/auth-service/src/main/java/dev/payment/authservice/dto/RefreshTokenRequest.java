package dev.payment.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}