package dev.payment.paymentservice.integration.processor;

public record PaymentProcessorCaptureResponse(
        String providerPaymentId,
        String providerSignature,
        String providerReference,
        boolean simulated
) {
}
