package dev.payment.combinedservice.payment.dto.response;

public record InitiatePaymentResponse(
    String transactionId,
    String status,
    String redirectUrl,
    Integer progressPercentage,
    String statusMessage
) {
    public static InitiatePaymentResponse pending(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "PENDING_OTP", null, 20, "Awaiting payment initiation");
    }

    public static InitiatePaymentResponse awaitingAuth(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "AUTHORIZATION_PENDING", null, 50, "Waiting for bank verification");
    }

    public static InitiatePaymentResponse processing(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "PROCESSING", null, 70, "Processing payment");
    }

    public static InitiatePaymentResponse completed(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "CAPTURED", null, 100, "Payment successful");
    }

    public static InitiatePaymentResponse failed(String transactionId, String reason) {
        return new InitiatePaymentResponse(transactionId, "FAILED", null, 0, reason);
    }

    public static InitiatePaymentResponse fromStatus(String status, String transactionId, String checkoutUrl) {
        return switch (status) {
            case "PENDING" -> pending(transactionId);
            case "CREATED" -> new InitiatePaymentResponse(transactionId, "CREATED", checkoutUrl, 10, "Payment created");
            case "AUTHORIZATION_PENDING" -> awaitingAuth(transactionId);
            case "AUTHORIZED" -> new InitiatePaymentResponse(transactionId, "AUTHORIZED", null, 80, "Payment authorized");
            case "PROCESSING" -> processing(transactionId);
            case "CAPTURED" -> completed(transactionId);
            case "FAILED" -> failed(transactionId, "Payment failed");
            case "EXPIRED" -> failed(transactionId, "Payment expired");
            case "REFUNDED" -> new InitiatePaymentResponse(transactionId, "REFUNDED", null, 100, "Payment refunded");
            case "PARTIALLY_REFUNDED" -> new InitiatePaymentResponse(transactionId, "PARTIALLY_REFUNDED", null, 100, "Partially refunded");
            default -> new InitiatePaymentResponse(transactionId, status, checkoutUrl, 0, "Processing payment");
        };
    }
}
