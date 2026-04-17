package dev.payment.paymentservice.integration.processor;

public record PaymentProcessorIntentResponse(
        String providerOrderId,
        String checkoutUrl,
        boolean simulated
) {
}
