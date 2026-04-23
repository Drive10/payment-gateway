package dev.payment.combinedservice.payment.dto.request;

public record CapturePaymentRequest(
        String providerPaymentId,
        String providerSignature,
        String otp
) {
}
