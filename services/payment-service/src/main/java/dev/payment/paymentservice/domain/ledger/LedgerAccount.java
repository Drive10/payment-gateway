package dev.payment.paymentservice.domain.ledger;

import dev.payment.paymentservice.domain.enums.LedgerAccountType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts", indexes = {
        @Index(name = "idx_ledger_accounts_merchant", columnList = "merchant_id"),
        @Index(name = "idx_ledger_accounts_type", columnList = "account_type")
})
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private LedgerAccountType accountType;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "locked_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedBalance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public LedgerAccount() {}

    public LedgerAccount(LedgerAccountType accountType, UUID merchantId, String currency) {
        this.accountType = accountType;
        this.merchantId = merchantId;
        this.currency = currency;
        this.balance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.lockedBalance = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LedgerAccountType getAccountType() { return accountType; }
    public void setAccountType(LedgerAccountType accountType) { this.accountType = accountType; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }

    public BigDecimal getLockedBalance() { return lockedBalance; }
    public void setLockedBalance(BigDecimal lockedBalance) { this.lockedBalance = lockedBalance; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}