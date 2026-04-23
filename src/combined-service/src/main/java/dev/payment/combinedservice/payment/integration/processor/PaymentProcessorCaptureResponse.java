package dev.payment.combinedservice.payment.integration.processor;

public record PaymentProcessorCaptureResponse(
        String providerPaymentId,
        String providerSignature,
        String providerReference,
        boolean simulated
) {
}
