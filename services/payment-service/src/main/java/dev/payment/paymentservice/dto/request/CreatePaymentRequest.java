package dev.payment.paymentservice.dto.request;

import dev.payment.paymentservice.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull PaymentMethod method,
        @NotBlank @Size(max = 32) String provider,
        @Size(max = 255) String notes
) {
}
