package dev.payment.notificationservice.service;

public class WebhookDeliveryException extends RuntimeException {
    
    public WebhookDeliveryException(String message) {
        super(message);
    }
    
    public WebhookDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
