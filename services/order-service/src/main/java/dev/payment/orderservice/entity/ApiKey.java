package dev.payment.orderservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true)
    private String keyHash;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;

    @Column(name = "rate_limit_per_minute")
    private Integer rateLimitPerMinute = 100;

    @Column(name = "rate_limit_per_day")
    private Integer rateLimitPerDay = 10000;

    @Column(name = "ip_whitelist", columnDefinition = "TEXT")
    private String ipWhitelist;

    @Column(name = "referer_whitelist", columnDefinition = "TEXT")
    private String refererWhitelist;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "last_used_ip")
    private String lastUsedIp;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public Integer getRateLimitPerDay() { return rateLimitPerDay; }
    public void setRateLimitPerDay(Integer rateLimitPerDay) { this.rateLimitPerDay = rateLimitPerDay; }

    public String getIpWhitelist() { return ipWhitelist; }
    public void setIpWhitelist(String ipWhitelist) { this.ipWhitelist = ipWhitelist; }

    public String getRefererWhitelist() { return refererWhitelist; }
    public void setRefererWhitelist(String refererWhitelist) { this.refererWhitelist = refererWhitelist; }

    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public String getLastUsedIp() { return lastUsedIp; }
    public void setLastUsedIp(String lastUsedIp) { this.lastUsedIp = lastUsedIp; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(Instant rotatedAt) { this.rotatedAt = rotatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
