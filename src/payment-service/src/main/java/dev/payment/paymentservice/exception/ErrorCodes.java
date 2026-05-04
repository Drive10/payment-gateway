package dev.payment.paymentservice.exception;

public class ErrorCodes {
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String PAYMENT_NOT_FOUND = "PAYMENT_NOT_FOUND";
    public static final String INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String REFUND_FAILED = "REFUND_FAILED";
    public static final String IDEMPOTENCY_KEY_CONFLICT = "IDEMPOTENCY_KEY_CONFLICT";
    public static final String INVALID_CURRENCY = "INVALID_CURRENCY";
    public static final String CURRENCY_NOT_SUPPORTED = "CURRENCY_NOT_SUPPORTED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    // Security error codes
    public static final String INVALID_API_KEY = "INVALID_API_KEY";
    public static final String API_KEY_REVOKED = "API_KEY_REVOKED";
    public static final String API_KEY_EXPIRED = "API_KEY_EXPIRED";
    public static final String API_KEY_SCOPE_DENIED = "API_KEY_SCOPE_DENIED";
    public static final String INVALID_WEBHOOK_SIGNATURE = "INVALID_WEBHOOK_SIGNATURE";
    public static final String WEBHOOK_TIMESTAMP_EXPIRED = "WEBHOOK_TIMESTAMP_EXPIRED";
    public static final String WEBHOOK_REPLAY_DETECTED = "WEBHOOK_REPLAY_DETECTED";
    public static final String INVALID_IP_whitelist = "INVALID_IP_WHITELIST";
    public static final String CONSTANT_TIME_CHECK_FAILED = "CONSTANT_TIME_CHECK_FAILED";
}