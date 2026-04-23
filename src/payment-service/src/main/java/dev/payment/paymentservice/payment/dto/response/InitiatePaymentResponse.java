package dev.payment.paymentservice.payment.dto.response;

public record InitiatePaymentResponse(
    String transactionId,
    String status,
    String checkoutUrl,
    String redirectUrl,
    Integer progressPercentage,
    String message,
    String statusMessage
) {
    public static InitiatePaymentResponse pending(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "PENDING_OTP", null, null, 20,
                "Awaiting payment initiation", "Awaiting payment initiation");
    }

    public static InitiatePaymentResponse awaitingAuth(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "AUTHORIZATION_PENDING", null, null, 50,
                "Waiting for bank verification", "Waiting for bank verification");
    }

    public static InitiatePaymentResponse processing(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "PROCESSING", null, null, 70,
                "Processing payment", "Processing payment");
    }

    public static InitiatePaymentResponse completed(String transactionId) {
        return new InitiatePaymentResponse(transactionId, "CAPTURED", null, null, 100,
                "Payment successful", "Payment successful");
    }

    public static InitiatePaymentResponse failed(String transactionId, String reason) {
        return new InitiatePaymentResponse(transactionId, "FAILED", null, null, 0, reason, reason);
    }

    public static InitiatePaymentResponse fromStatus(String status, String transactionId, String checkoutUrl) {
        return switch (status) {
            case "PENDING" -> pending(transactionId);
            case "CREATED" -> new InitiatePaymentResponse(transactionId, "CREATED", checkoutUrl, checkoutUrl, 10,
                    "Payment created", "Payment created");
            case "AWAITING_UPI_PAYMENT" -> new InitiatePaymentResponse(transactionId, "AWAITING_UPI_PAYMENT", checkoutUrl, checkoutUrl, 30,
                    "Awaiting UPI collect completion", "Awaiting UPI collect completion");
            case "AUTHORIZATION_PENDING" -> awaitingAuth(transactionId);
            case "AUTHORIZED" -> new InitiatePaymentResponse(transactionId, "AUTHORIZED", null, null, 80,
                    "Payment authorized", "Payment authorized");
            case "PROCESSING" -> processing(transactionId);
            case "CAPTURED" -> completed(transactionId);
            case "FAILED" -> failed(transactionId, "Payment failed");
            case "EXPIRED" -> failed(transactionId, "Payment expired");
            case "REFUNDED" -> new InitiatePaymentResponse(transactionId, "REFUNDED", null, null, 100,
                    "Payment refunded", "Payment refunded");
            case "PARTIALLY_REFUNDED" -> new InitiatePaymentResponse(transactionId, "PARTIALLY_REFUNDED", null, null, 100,
                    "Partially refunded", "Partially refunded");
            default -> new InitiatePaymentResponse(transactionId, status, checkoutUrl, checkoutUrl, 0,
                    "Processing payment", "Processing payment");
        };
    }
}
