package dev.payment.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "settlement_reference", unique = true, nullable = false, length = 50)
    private String settlementReference;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "total_transactions", nullable = false)
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "total_fees", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Column(name = "total_refunds", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "payout_reference", length = 100)
    private String payoutReference;

    @Column(name = "payout_date")
    private LocalDateTime payoutDate;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (settlementReference == null) {
            settlementReference = "STL-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SettlementStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}
