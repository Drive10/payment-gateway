package dev.payment.common.audit;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String action;

    @Column
    private String userId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column
    private String ipAddress;

    @Column
    private String userAgent;

    @Column
    private String correlationId;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AuditLog auditLog = new AuditLog();

        public Builder entityType(String entityType) {
            auditLog.setEntityType(entityType);
            return this;
        }

        public Builder entityId(String entityId) {
            auditLog.setEntityId(entityId);
            return this;
        }

        public Builder action(String action) {
            auditLog.setAction(action);
            return this;
        }

        public Builder userId(String userId) {
            auditLog.setUserId(userId);
            return this;
        }

        public Builder oldValue(String oldValue) {
            auditLog.setOldValue(oldValue);
            return this;
        }

        public Builder newValue(String newValue) {
            auditLog.setNewValue(newValue);
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            auditLog.setIpAddress(ipAddress);
            return this;
        }

        public Builder userAgent(String userAgent) {
            auditLog.setUserAgent(userAgent);
            return this;
        }

        public Builder correlationId(String correlationId) {
            auditLog.setCorrelationId(correlationId);
            return this;
        }

        public AuditLog build() {
            return auditLog;
        }
    }
}
