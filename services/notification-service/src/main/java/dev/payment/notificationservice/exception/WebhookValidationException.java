package dev.payment.notificationservice.exception;

public class WebhookValidationException extends RuntimeException {

    private final String errorCode;

    public WebhookValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
