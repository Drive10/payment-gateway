package dev.payment.paymentservice.payment.integration.processor;

public record PaymentProcessorCaptureResponse(
        String providerPaymentId,
        String providerSignature,
        String providerReference,
        boolean simulated
) {
}
