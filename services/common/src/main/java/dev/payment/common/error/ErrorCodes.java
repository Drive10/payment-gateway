package dev.payment.common.error;

public class ErrorCodes {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String DUPLICATE_RESOURCE = "DUPLICATE_RESOURCE";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
    public static final String PAYMENT_DECLINED = "PAYMENT_DECLINED";
    public static final String PAYMENT_EXPIRED = "PAYMENT_EXPIRED";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String INVALID_CARD = "INVALID_CARD";
    public static final String CARD_DECLINED = "CARD_DECLINED";
    public static final String REFUND_EXCEEDED = "REFUND_EXCEEDED";
    public static final String REFUND_NOT_ALLOWED = "REFUND_NOT_ALLOWED";
    public static final String MERCHANT_NOT_FOUND = "MERCHANT_NOT_FOUND";
    public static final String MERCHANT_SUSPENDED = "MERCHANT_SUSPENDED";
    public static final String INVALID_API_KEY = "INVALID_API_KEY";
    public static final String SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String CIRCUIT_BREAKER_OPEN = "CIRCUIT_BREAKER_OPEN";
    public static final String TIMEOUT_ERROR = "TIMEOUT_ERROR";

    private ErrorCodes() {}
}
