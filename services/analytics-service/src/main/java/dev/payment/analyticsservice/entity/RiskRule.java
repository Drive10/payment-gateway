package dev.payment.analyticsservice.entity;
import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_rules")
@Data
public class RiskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "rule_type", nullable = false, length = 50)
    private String ruleType;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String conditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskAction action = RiskAction.REVIEW;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer priority = 0;

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

    public RiskRule() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }
    public RiskAction getAction() { return action; }
    public void setAction(RiskAction action) { this.action = action; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum RiskAction {
        APPROVE, REVIEW, REJECT
    }
}
