package dev.payment.analyticsservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_settlements")
public class MerchantSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", unique = true, nullable = false)
    private UUID merchantId;

    @Column(name = "merchant_name", nullable = false)
    private String merchantName;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "total_settled", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSettled = BigDecimal.ZERO;

    @Column(name = "settlement_frequency", nullable = false, length = 20)
    private String settlementFrequency = "DAILY";

    @Column(name = "auto_settle", nullable = false)
    private Boolean autoSettle = true;

    @Column(name = "minimum_settlement", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumSettlement = new BigDecimal("1000");

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public MerchantSettlement() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public BigDecimal getPendingBalance() { return pendingBalance; }
    public void setPendingBalance(BigDecimal pendingBalance) { this.pendingBalance = pendingBalance; }
    public BigDecimal getTotalSettled() { return totalSettled; }
    public void setTotalSettled(BigDecimal totalSettled) { this.totalSettled = totalSettled; }
    public String getSettlementFrequency() { return settlementFrequency; }
    public void setSettlementFrequency(String settlementFrequency) { this.settlementFrequency = settlementFrequency; }
    public Boolean getAutoSettle() { return autoSettle; }
    public void setAutoSettle(Boolean autoSettle) { this.autoSettle = autoSettle; }
    public BigDecimal getMinimumSettlement() { return minimumSettlement; }
    public void setMinimumSettlement(BigDecimal minimumSettlement) { this.minimumSettlement = minimumSettlement; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
