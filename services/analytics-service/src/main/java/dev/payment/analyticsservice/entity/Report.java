package dev.payment.analyticsservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_type", nullable = false, length = 100)
    private String reportType;

    @Column(name = "report_name", nullable = false, length = 255)
    private String reportName;

    @Column(name = "report_params", columnDefinition = "jsonb")
    private String reportParams;

    @Column(name = "report_data", columnDefinition = "jsonb")
    private String reportData;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @Column(name = "merchant_id", length = 36)
    private String merchantId;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private Instant createdAt;

    public Report() {
        this.createdAt = Instant.now();
        this.reportParams = "{}";
        this.reportData = "{}";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getReportParams() { return reportParams; }
    public void setReportParams(String reportParams) { this.reportParams = reportParams; }

    public String getReportData() { return reportData; }
    public void setReportData(String reportData) { this.reportData = reportData; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
