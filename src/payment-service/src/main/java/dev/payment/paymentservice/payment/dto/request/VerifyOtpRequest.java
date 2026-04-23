package dev.payment.paymentservice.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(
    @NotBlank String transactionId,
    @NotBlank @Size(min = 6, max = 6) String otp
) {
}
