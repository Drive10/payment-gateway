package dev.payflow.gateway.dto;

public class PaymentLinkResponse {
    private String uuid;
    private String orderNumber;
    private String description;
    private java.math.BigDecimal amount;
    private String currency;
    private String status;
    private String expiresAt;
    private String paymentUrl;

    public PaymentLinkResponse() {}
    
    public PaymentLinkResponse(String uuid, String orderNumber, String description, 
            java.math.BigDecimal amount, String currency, String status, String expiresAt, String paymentUrl) {
        this.uuid = uuid;
        this.orderNumber = orderNumber;
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.expiresAt = expiresAt;
        this.paymentUrl = paymentUrl;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
}
