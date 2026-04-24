package dev.payment.paymentservice.payment.dto.request;

import dev.payment.common.dto.OrderSnapshot;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePaymentRequest(
        UUID orderId,
        UUID merchantId,
        OrderSnapshot order,
        @NotNull PaymentMethod method,
        @NotBlank @Size(max = 32) String provider,
        TransactionMode transactionMode,
        @Size(max = 255) String notes
) {
    public static CreatePaymentRequest createLegacy(UUID orderId, UUID merchantId, PaymentMethod method, String provider, TransactionMode transactionMode, String notes) {
        return new CreatePaymentRequest(orderId, merchantId, null, method, provider, transactionMode, notes);
    }

    public boolean isNewFormat() {
        return order != null && order.id() != null;
    }
}