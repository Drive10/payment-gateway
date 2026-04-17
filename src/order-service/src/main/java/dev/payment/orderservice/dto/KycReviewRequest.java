package dev.payment.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record KycReviewRequest(
        String reviewer
) {
    public KycReviewRequest {
        if (reviewer == null || reviewer.isBlank()) {
            reviewer = "SYSTEM";
        }
    }
}
