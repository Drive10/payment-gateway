package dev.payment.riskservice.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "merchant_reference", nullable = false, length = 80)
    private String merchantReference;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "country_code", nullable = false, length = 8)
    private String countryCode;
    @Column(name = "velocity_count", nullable = false)
    private int velocityCount;
    @Column(name = "risk_score", nullable = false)
    private int riskScore;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Decision decision;
    @Column(nullable = false, length = 255)
    private String reasons;
    public UUID getId() { return id; }
    public String getMerchantReference() { return merchantReference; }
    public void setMerchantReference(String merchantReference) { this.merchantReference = merchantReference; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public int getVelocityCount() { return velocityCount; }
    public void setVelocityCount(int velocityCount) { this.velocityCount = velocityCount; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) { this.decision = decision; }
    public String getReasons() { return reasons; }
    public void setReasons(String reasons) { this.reasons = reasons; }
}
