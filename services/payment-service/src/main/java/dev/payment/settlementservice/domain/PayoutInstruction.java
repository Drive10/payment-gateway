package dev.payment.settlementservice.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payout_instructions")
public class PayoutInstruction extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "batch_reference", nullable = false, length = 80)
    private String batchReference;
    @Column(name = "beneficiary_account", nullable = false, length = 80)
    private String beneficiaryAccount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 32)
    private String status;
    public UUID getId() { return id; }
    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }
    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public void setBeneficiaryAccount(String beneficiaryAccount) { this.beneficiaryAccount = beneficiaryAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
