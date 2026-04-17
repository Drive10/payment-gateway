package dev.payment.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private String kycStatus = "PENDING";

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "kyc_verified_by")
    private String kycVerifiedBy;

    @Column(name = "verification_notes")
    private String verificationNotes;

    @Column(name = "settlement_schedule")
    @Builder.Default
    private String settlementSchedule = "DAILY";

    @Column(name = "settlement_enabled")
    @Builder.Default
    private Boolean settlementEnabled = true;

    @Column(name = "pricing_tier")
    @Builder.Default
    private String pricingTier = "STANDARD";

    @Column(name = "transaction_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal transactionLimit = DEFAULT_TRANSACTION_LIMIT;

    @Column(name = "daily_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal dailyLimit = DEFAULT_DAILY_LIMIT;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal monthlyLimit = DEFAULT_MONTHLY_LIMIT;

    @Column(name = "current_month_volume", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal currentMonthVolume = BigDecimal.ZERO;

    @Column(name = "current_month_transactions")
    @Builder.Default
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
    @Builder.Default
    private String status = "ACTIVE";

    @Column(columnDefinition = "jsonb")
    @Builder.Default
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
}
