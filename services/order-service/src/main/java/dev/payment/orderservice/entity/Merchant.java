package dev.payment.orderservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
public class Merchant {
    private static final BigDecimal DEFAULT_TRANSACTION_LIMIT = new BigDecimal("100000");
    private static final BigDecimal DEFAULT_DAILY_LIMIT = new BigDecimal("1000000");
    private static final BigDecimal DEFAULT_MONTHLY_LIMIT = new BigDecimal("5000000");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String phone;

    @Column
    private String website;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "business_category")
    private String businessCategory;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "kyc_status", nullable = false)
    private String kycStatus = "PENDING";

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "kyc_verified_by")
    private String kycVerifiedBy;

    @Column(name = "verification_notes")
    private String verificationNotes;

    @Column(name = "settlement_schedule")
    private String settlementSchedule = "DAILY";

    @Column(name = "settlement_enabled")
    private Boolean settlementEnabled = true;

    @Column(name = "pricing_tier")
    private String pricingTier = "STANDARD";

    @Column(name = "transaction_limit", precision = 19, scale = 4)
    private BigDecimal transactionLimit = DEFAULT_TRANSACTION_LIMIT;

    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit = DEFAULT_DAILY_LIMIT;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit = DEFAULT_MONTHLY_LIMIT;

    @Column(name = "current_month_volume", precision = 19, scale = 4)
    private BigDecimal currentMonthVolume = BigDecimal.ZERO;

    @Column(name = "current_month_transactions")
    private Integer currentMonthTransactions = 0;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "api_key", unique = true)
    private String apiKey;

    @Column(name = "api_secret_hash")
    private String apiSecretHash;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(columnDefinition = "jsonb")
    private String metadata = "{}";

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }

    public String getTaxId() { return taxId; }
    public void setTaxId(String taxId) { this.taxId = taxId; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public Instant getKycVerifiedAt() { return kycVerifiedAt; }
    public void setKycVerifiedAt(Instant kycVerifiedAt) { this.kycVerifiedAt = kycVerifiedAt; }

    public String getKycVerifiedBy() { return kycVerifiedBy; }
    public void setKycVerifiedBy(String kycVerifiedBy) { this.kycVerifiedBy = kycVerifiedBy; }

    public String getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(String verificationNotes) { this.verificationNotes = verificationNotes; }

    public String getSettlementSchedule() { return settlementSchedule; }
    public void setSettlementSchedule(String settlementSchedule) { this.settlementSchedule = settlementSchedule; }

    public Boolean getSettlementEnabled() { return settlementEnabled; }
    public void setSettlementEnabled(Boolean settlementEnabled) { this.settlementEnabled = settlementEnabled; }

    public String getPricingTier() { return pricingTier; }
    public void setPricingTier(String pricingTier) { this.pricingTier = pricingTier; }

    public BigDecimal getTransactionLimit() { return transactionLimit; }
    public void setTransactionLimit(BigDecimal transactionLimit) { this.transactionLimit = transactionLimit; }

    public BigDecimal getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(BigDecimal dailyLimit) { this.dailyLimit = dailyLimit; }

    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public BigDecimal getCurrentMonthVolume() { return currentMonthVolume; }
    public void setCurrentMonthVolume(BigDecimal currentMonthVolume) { this.currentMonthVolume = currentMonthVolume; }

    public Integer getCurrentMonthTransactions() { return currentMonthTransactions; }
    public void setCurrentMonthTransactions(Integer currentMonthTransactions) { this.currentMonthTransactions = currentMonthTransactions; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecretHash() { return apiSecretHash; }
    public void setApiSecretHash(String apiSecretHash) { this.apiSecretHash = apiSecretHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
