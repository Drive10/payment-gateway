package dev.payment.combinedservice.payment.integration.processor;

public record PaymentProcessorIntentResponse(
        String providerOrderId,
        String checkoutUrl,
        boolean simulated
) {
}
