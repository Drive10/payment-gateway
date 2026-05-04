package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "batch_id", nullable = false, unique = true)
    private String batchId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementStatus status;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(name = "payout_method")
    private String payoutMethod;

    @Column(name = "bank_reference")
    private String bankReference;

    @Column(name = "failure_reason")
    private String failureReason;

    public enum SettlementStatus {
        PENDING,
        APPROVED,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (batchId == null) batchId = UUID.randomUUID().toString();
        if (feeAmount == null) feeAmount = BigDecimal.ZERO;
        if (netAmount == null) netAmount = BigDecimal.ZERO;
        if (status == null) status = SettlementStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == SettlementStatus.COMPLETED;
    }

    public boolean canApprove() {
        return status == SettlementStatus.PENDING;
    }

    public boolean canProcess() {
        return status == SettlementStatus.APPROVED;
    }
}