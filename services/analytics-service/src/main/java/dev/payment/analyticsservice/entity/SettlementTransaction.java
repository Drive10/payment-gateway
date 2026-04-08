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
@Table(name = "settlement_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "settlement_id", nullable = false)
    private UUID settlementId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "transaction_reference", nullable = false, length = 100)
    private String transactionReference;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "transaction_type", nullable = false, length = 20)
    @Builder.Default
    private String transactionType = "PAYMENT";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransactionStatus {
        PENDING, INCLUDED, EXCLUDED, SETTLED
    }
}