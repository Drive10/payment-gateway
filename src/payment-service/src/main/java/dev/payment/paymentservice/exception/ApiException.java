package dev.payment.paymentservice.exception;

import dev.payment.common.exception.PayFlowException;
import org.springframework.http.HttpStatus;

/**
 * Payment service specific API exception.
 * Extends PayFlowException for standardized error handling while maintaining backward compatibility.
 */
public class ApiException extends PayFlowException {

    public ApiException(HttpStatus status, String code, String message) {
        super(message, code, status.value(), true);
    }

    public ApiException(HttpStatus status, String code, Throwable cause, String message) {
        super(message, cause, code, status.value(), true);
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException serviceUnavailable(String code, String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }
}
