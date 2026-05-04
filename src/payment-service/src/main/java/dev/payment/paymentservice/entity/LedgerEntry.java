package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entry_id", nullable = false, unique = true)
    private String entryId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "journal_id")
    private UUID journalId;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "reference", unique = true)
    private String reference;

    @Column(name = "refund_id")
    private String refundId;

    public enum AccountType {
        MERCHANT_RECEivable,
        PLATFORM_RECEIVABLE,
        PAYMENT_GATEWAY,
        PLATFORM_FEE_RECEIVABLE,
        CUSTOMER_ESCROW,
        SETTLEMENT_HOLD,
        PAYOUT_ACCOUNT,
        REFUND_RESERVE
    }

    public enum EntryType {
        DEBIT,
        CREDIT
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (entryId == null) entryId = UUID.randomUUID().toString();
    }

    public boolean isDebit() {
        return entryType == EntryType.DEBIT;
    }

    public boolean isCredit() {
        return entryType == EntryType.CREDIT;
    }
}