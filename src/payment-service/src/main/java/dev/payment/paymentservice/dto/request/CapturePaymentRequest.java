package dev.payment.paymentservice.dto.request;

public record CapturePaymentRequest(
        String providerPaymentId,
        String providerSignature,
        String otp
) {
}
