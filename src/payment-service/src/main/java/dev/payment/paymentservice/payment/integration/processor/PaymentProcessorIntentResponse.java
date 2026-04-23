package dev.payment.paymentservice.payment.integration.processor;

public record PaymentProcessorIntentResponse(
        String providerOrderId,
        String checkoutUrl,
        boolean simulated
) {
}
