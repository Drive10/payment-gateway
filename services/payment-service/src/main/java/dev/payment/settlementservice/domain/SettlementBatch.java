package dev.payment.settlementservice.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "settlement_batches")
public class SettlementBatch extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "batch_reference", nullable = false, unique = true, length = 80)
    private String batchReference;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SettlementStatus status;
    public UUID getId() { return id; }
    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
}
