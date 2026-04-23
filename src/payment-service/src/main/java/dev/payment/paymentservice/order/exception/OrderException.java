package dev.payment.paymentservice.order.exception;

public class OrderException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public OrderException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public static OrderException notFound(String message) {
        return new OrderException(message, "ORDER_NOT_FOUND", 404);
    }

    public static OrderException invalidState(String message) {
        return new OrderException(message, "INVALID_ORDER_STATE", 400);
    }

    public static OrderException forbidden(String message) {
        return new OrderException(message, "ORDER_FORBIDDEN", 403);
    }
}