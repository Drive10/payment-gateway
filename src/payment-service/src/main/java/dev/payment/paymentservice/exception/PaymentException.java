package dev.payment.paymentservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class PaymentException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public PaymentException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static PaymentException notFound(String message) {
        return new PaymentException(message, HttpStatus.NOT_FOUND, ErrorCodes.PAYMENT_NOT_FOUND);
    }

    public static PaymentException badRequest(String message) {
        return new PaymentException(message, HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_REQUEST);
    }

    public static PaymentException conflict(String message) {
        return new PaymentException(message, HttpStatus.CONFLICT, ErrorCodes.IDEMPOTENCY_KEY_CONFLICT);
    }

    public static PaymentException paymentFailed(String message) {
        return new PaymentException(message, HttpStatus.PAYMENT_REQUIRED, ErrorCodes.PAYMENT_FAILED);
    }

    public static PaymentException internal(String message) {
        return new PaymentException(message, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR);
    }
}