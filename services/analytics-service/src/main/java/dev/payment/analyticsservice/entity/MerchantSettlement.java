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
@Table(name = "merchant_settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantSettlement {

    private static final BigDecimal DEFAULT_MINIMUM_SETTLEMENT = new BigDecimal("1000");

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
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "total_settled", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal totalSettled = BigDecimal.ZERO;

    @Column(name = "settlement_frequency", nullable = false, length = 20)
    @Builder.Default
    private String settlementFrequency = "DAILY";

    @Column(name = "auto_settle", nullable = false)
    @Builder.Default
    private Boolean autoSettle = true;

    @Column(name = "minimum_settlement", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal minimumSettlement = DEFAULT_MINIMUM_SETTLEMENT;

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
}
