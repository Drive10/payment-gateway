package dev.payment.analyticsservice.entity;
import lombok.Data;

import jakarta.persistence.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "analytics_events")
@Data
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_category", nullable = false, length = 50)
    private String eventCategory;

    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private String eventData;

    @Column(name = "merchant_id", length = 36)
    private String merchantId;

    @Column(name = "order_id", length = 36)
    private String orderId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "amount", precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    public AnalyticsEvent() {
        this.eventData = "{}";
        this.metadata = "{}";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventCategory() { return eventCategory; }
    public void setEventCategory(String eventCategory) { this.eventCategory = eventCategory; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
