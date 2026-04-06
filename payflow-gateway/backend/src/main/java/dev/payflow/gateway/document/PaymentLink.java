package dev.payflow.gateway.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "payment_links")
public class PaymentLink {
    @Id
    private String id;
    private String merchantApiKey;
    private String uuid;
    private String orderNumber;
    private String description;
    private BigDecimal amount;
    private String currency;
    private PaymentMode mode;
    private LinkStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PaymentMode {
        TEST, LIVE
    }

    public enum LinkStatus {
        ACTIVE, INACTIVE, EXPIRED, PAID
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMerchantApiKey() { return merchantApiKey; }
    public void setMerchantApiKey(String merchantApiKey) { this.merchantApiKey = merchantApiKey; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentMode getMode() { return mode; }
    public void setMode(PaymentMode mode) { this.mode = mode; }
    public LinkStatus getStatus() { return status; }
    public void setStatus(LinkStatus status) { this.status = status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
