package dev.payment.paymentservice.dto.response;

public record InitiatePaymentResponse(
    String transactionId,
    String status,
    String redirectUrl
) {
    public static InitiatePaymentResponse pending(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "PENDING_OTP", null);
    }

    public static InitiatePaymentResponse completed(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "COMPLETED", null);
    }

    public static InitiatePaymentResponse failed(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "FAILED", null);
    }
}
