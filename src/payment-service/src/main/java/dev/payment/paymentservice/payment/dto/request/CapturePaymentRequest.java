package dev.payment.paymentservice.payment.dto.request;

public record CapturePaymentRequest(
        String providerPaymentId,
        String providerSignature,
        String otp
) {
}
