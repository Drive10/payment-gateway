package dev.payment.combinedservice.order.dto;

import java.util.UUID;

public record InitiatePaymentResponse(
        UUID paymentId,
        UUID orderId,
        String status,
        String redirectUrl
) {
}
