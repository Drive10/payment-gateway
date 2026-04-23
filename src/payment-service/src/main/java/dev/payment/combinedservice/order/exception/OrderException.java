package dev.payment.combinedservice.order.exception;

import org.springframework.http.HttpStatus;

public class OrderException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public OrderException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.code = "ORDER_ERROR";
    }

    public OrderException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
