package dev.payment.paymentservice.payment.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StripeWebhookRequest(
        @JsonProperty("id") String id,
        @JsonProperty("object") String object,
        @JsonProperty("type") String type,
        @JsonProperty("created") Long created,
        @JsonProperty("data") StripeEventData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StripeEventData(
            @JsonProperty("object") StripeObject object
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StripeObject(
            @JsonProperty("id") String id,
            @JsonProperty("object") String object,
            @JsonProperty("amount") Long amount,
            @JsonProperty("currency") String currency,
            @JsonProperty("status") String status,
            @JsonProperty("customer") String customer,
            @JsonProperty("metadata") java.util.Map<String, String> metadata,
            @JsonProperty("payment_intent") String paymentIntent,
            @JsonProperty("payment_method") String paymentMethod,
            @JsonProperty("capture_method") String captureMethod,
            @JsonProperty("amount_captured") Long amountCaptured,
            @JsonProperty("amount_refunded") Long amountRefunded,
            @JsonProperty("reason") String reason,
            @JsonProperty("payment_intent") String refundPaymentIntent,
            @JsonProperty("payment_method_details") java.util.Map<String, Object> paymentMethodDetails
    ) {}
}