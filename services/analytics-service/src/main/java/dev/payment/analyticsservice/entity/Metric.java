package dev.payment.analyticsservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "metrics")
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "dimension_merchant_id", length = 36)
    private String dimensionMerchantId;

    @Column(name = "dimension_currency", length = 3)
    private String dimensionCurrency;

    @Column(name = "dimension_status", length = 50)
    private String dimensionStatus;

    @Column(name = "dimension_period", length = 20)
    private String dimensionPeriod;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "count")
    private Long count = 1L;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;

    @Column(name = "sum_value")
    private Double sumValue;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Metric() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMetricName() { return metricName; }
    public void setMetricName(String metricName) { this.metricName = metricName; }

    public String getMetricType() { return metricType; }
    public void setMetricType(String metricType) { this.metricType = metricType; }

    public String getDimensionMerchantId() { return dimensionMerchantId; }
    public void setDimensionMerchantId(String dimensionMerchantId) { this.dimensionMerchantId = dimensionMerchantId; }

    public String getDimensionCurrency() { return dimensionCurrency; }
    public void setDimensionCurrency(String dimensionCurrency) { this.dimensionCurrency = dimensionCurrency; }

    public String getDimensionStatus() { return dimensionStatus; }
    public void setDimensionStatus(String dimensionStatus) { this.dimensionStatus = dimensionStatus; }

    public String getDimensionPeriod() { return dimensionPeriod; }
    public void setDimensionPeriod(String dimensionPeriod) { this.dimensionPeriod = dimensionPeriod; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }

    public Double getMinValue() { return minValue; }
    public void setMinValue(Double minValue) { this.minValue = minValue; }

    public Double getMaxValue() { return maxValue; }
    public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }

    public Double getSumValue() { return sumValue; }
    public void setSumValue(Double sumValue) { this.sumValue = sumValue; }

    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }

    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
