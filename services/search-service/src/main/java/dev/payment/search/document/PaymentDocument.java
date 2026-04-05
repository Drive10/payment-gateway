package dev.payment.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import java.time.Instant;
import java.util.Map;

@Document(indexName = "payments")
@Setting(shards = 1, replicas = 0)
public class PaymentDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String orderId;
    
    @Field(type = FieldType.Double)
    private Double amount;
    
    @Field(type = FieldType.Keyword)
    private String currency;
    
    @Field(type = FieldType.Keyword)
    private String paymentMethod;
    
    @Field(type = FieldType.Keyword)
    private String provider;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Boolean)
    private Boolean simulated;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String failureReason;
    
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
    
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;
    
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;
    
    @Field(type = FieldType.Keyword)
    private String merchantId;
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Boolean getSimulated() { return simulated; }
    public void setSimulated(Boolean simulated) { this.simulated = simulated; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
}
