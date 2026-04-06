package dev.payment.common.exception;

/**
 * Base exception class for all PayFlow service exceptions.
 * Provides standardized error codes and HTTP status mapping.
 */
public class PayFlowException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;
    private final boolean isOperational;

    public PayFlowException(String message, String errorCode, int httpStatus, boolean isOperational) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.isOperational = isOperational;
    }

    public PayFlowException(String message, Throwable cause, String errorCode, int httpStatus, boolean isOperational) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.isOperational = isOperational;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isOperational() {
        return isOperational;
    }

    /**
     * Operational errors are expected errors that can be safely returned to clients.
     * Programmatic errors are unexpected errors that should not leak internal details.
     */
    public static PayFlowException operational(String message, String errorCode) {
        return new PayFlowException(message, errorCode, 400, true);
    }

    public static PayFlowException notFound(String message) {
        return new PayFlowException(message, "RESOURCE_NOT_FOUND", 404, true);
    }

    public static PayFlowException badRequest(String message) {
        return new PayFlowException(message, "BAD_REQUEST", 400, true);
    }

    public static PayFlowException unauthorized(String message) {
        return new PayFlowException(message, "UNAUTHORIZED", 401, true);
    }

    public static PayFlowException forbidden(String message) {
        return new PayFlowException(message, "FORBIDDEN", 403, true);
    }

    public static PayFlowException conflict(String message) {
        return new PayFlowException(message, "CONFLICT", 409, true);
    }

    public static PayFlowException rateLimitExceeded(String message) {
        return new PayFlowException(message, "RATE_LIMIT_EXCEEDED", 429, true);
    }

    public static PayFlowException internal(String message) {
        return new PayFlowException(message, "INTERNAL_ERROR", 500, false);
    }

    public static PayFlowException serviceUnavailable(String message) {
        return new PayFlowException(message, "SERVICE_UNAVAILABLE", 503, true);
    }
}