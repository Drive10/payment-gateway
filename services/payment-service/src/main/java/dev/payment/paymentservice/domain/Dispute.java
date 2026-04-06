package dev.payment.paymentservice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "dispute_reference", unique = true, nullable = false, length = 50)
    private String disputeReference;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DisputeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DisputeReason reason;

    @Column(length = 500)
    private String description;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "chargeback_amount", precision = 19, scale = 2)
    private BigDecimal chargebackAmount;

    @Column(name = "chargeback_currency", length = 3)
    private String chargebackCurrency;

    @Column(name = "initiated_by", length = 50)
    private String initiatedBy;

    @Column(name = "initiated_at")
    private Instant initiatedAt;

    @Column(name = "due_by")
    private Instant dueBy;

    @Column(name = "won_at")
    private Instant wonAt;

    @Column(name = "lost_at")
    private Instant lostAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (disputeReference == null) {
            disputeReference = "DSP-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum DisputeStatus {
        OPEN("Dispute opened, awaiting evidence"),
        UNDER_REVIEW("Evidence submitted, under review"),
        ESCALATED("Escalated to arbitration"),
        WON("Merchant won dispute"),
        LOST("Merchant lost dispute"),
        CLOSED("Dispute closed");

        private final String description;

        DisputeStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum DisputeReason {
        DUPLICATE("Duplicate charge"),
        FRAUD("Fraudulent transaction"),
        SUBSCRIPTION_CANCELLED("Subscription cancelled"),
        PRODUCT_NOT_RECEIVED("Product not received"),
        PRODUCT_UNACCEPTABLE("Product unacceptable"),
        UNRECOGNIZED("Unrecognized transaction"),
        OTHER("Other reason");

        private final String description;

        DisputeReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDisputeReference() { return disputeReference; }
    public void setDisputeReference(String disputeReference) { this.disputeReference = disputeReference; }

    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }

    public DisputeReason getReason() { return reason; }
    public void setReason(DisputeReason reason) { this.reason = reason; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public BigDecimal getChargebackAmount() { return chargebackAmount; }
    public void setChargebackAmount(BigDecimal chargebackAmount) { this.chargebackAmount = chargebackAmount; }

    public String getChargebackCurrency() { return chargebackCurrency; }
    public void setChargebackCurrency(String chargebackCurrency) { this.chargebackCurrency = chargebackCurrency; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public Instant getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(Instant initiatedAt) { this.initiatedAt = initiatedAt; }

    public Instant getDueBy() { return dueBy; }
    public void setDueBy(Instant dueBy) { this.dueBy = dueBy; }

    public Instant getWonAt() { return wonAt; }
    public void setWonAt(Instant wonAt) { this.wonAt = wonAt; }

    public Instant getLostAt() { return lostAt; }
    public void setLostAt(Instant lostAt) { this.lostAt = lostAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
