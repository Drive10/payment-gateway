package dev.payment.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public class SmsRequest {
    
    @NotBlank(message = "Phone number is required")
    private String to;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String userId;
    private String paymentId;
    private String orderId;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
