package dev.payment.paymentservice.payment.dto.request;

import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull UUID merchantId,
        @NotNull PaymentMethod method,
        @NotBlank @Size(max = 32) String provider,
        TransactionMode transactionMode,
        @Size(max = 255) String notes
) {
}
