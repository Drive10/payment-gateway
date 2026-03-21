package dev.payment.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CapturePaymentRequest(
        @NotBlank @Size(max = 64) String providerPaymentId,
        @NotBlank @Size(max = 255) String providerSignature
) {
}
