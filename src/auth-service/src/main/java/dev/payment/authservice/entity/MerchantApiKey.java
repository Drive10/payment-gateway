package dev.payment.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "merchant_api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantApiKey {

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 10)
    private String keyPrefix;

    @Column(name = "name")
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scopes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Boolean> scopes = Map.of("payments:read", true, "payments:write", true);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ip_whitelist", columnDefinition = "jsonb")
    private Map<String, Boolean> ipWhitelist;

    @Column(name = "rate_limit")
    @Builder.Default
    private Integer rateLimit = 1000;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_rotated_at")
    private Instant lastRotatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason")
    private String revokedReason;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    public boolean hasScope(String scope) {
        return scopes != null && Boolean.TRUE.equals(scopes.get(scope));
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }
}