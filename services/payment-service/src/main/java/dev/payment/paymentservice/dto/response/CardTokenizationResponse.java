package dev.payment.paymentservice.dto.response;

public record CardTokenizationResponse(
    String token,
    String lastFour,
    String cardBrand
) {
    public CardTokenizationResponse(String token) {
        this(token, token.substring(token.length() - 4), detectBrand(token));
    }
    
    private static String detectBrand(String token) {
        if (token.startsWith("4")) return "VISA";
        if (token.startsWith("5")) return "MASTERCARD";
        if (token.startsWith("3")) return "AMEX";
        return "UNKNOWN";
    }
}