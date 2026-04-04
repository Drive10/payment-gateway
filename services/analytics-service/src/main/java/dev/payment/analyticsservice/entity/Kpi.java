package dev.payment.analyticsservice.entity;
import lombok.Data;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kpis")
@Data
public class Kpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kpi_name", nullable = false, unique = true, length = 100)
    private String kpiName;

    @Column(name = "kpi_value", nullable = false)
    private Double kpiValue;

    @Column(name = "kpi_unit", length = 20)
    private String kpiUnit;

    @Column(name = "previous_value")
    private Double previousValue;

    @Column(name = "change_percentage")
    private Double changePercentage;

    @Column(name = "dimension_merchant_id", length = 36)
    private String dimensionMerchantId;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Kpi() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKpiName() { return kpiName; }
    public void setKpiName(String kpiName) { this.kpiName = kpiName; }

    public Double getKpiValue() { return kpiValue; }
    public void setKpiValue(Double kpiValue) { this.kpiValue = kpiValue; }

    public String getKpiUnit() { return kpiUnit; }
    public void setKpiUnit(String kpiUnit) { this.kpiUnit = kpiUnit; }

    public Double getPreviousValue() { return previousValue; }
    public void setPreviousValue(Double previousValue) { this.previousValue = previousValue; }

    public Double getChangePercentage() { return changePercentage; }
    public void setChangePercentage(Double changePercentage) { this.changePercentage = changePercentage; }

    public String getDimensionMerchantId() { return dimensionMerchantId; }
    public void setDimensionMerchantId(String dimensionMerchantId) { this.dimensionMerchantId = dimensionMerchantId; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
