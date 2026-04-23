package dev.payment.combinedservice.order.dto;

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
