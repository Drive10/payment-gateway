package dev.payment.paymentservice.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record RazorpayWebhookRequest(
        String event,
        Payload payload
) {
    public record Payload(PaymentPayload payment, RefundPayload refund) {}

    public record PaymentPayload(EntityWrapper entity) {}

    public record RefundPayload(EntityWrapper entity) {}

    public record EntityWrapper(
            String id,
            @JsonProperty("order_id") String orderId,
            @JsonProperty("payment_id") String paymentId,
            BigDecimal amount,
            String notes
    ) {}
}
