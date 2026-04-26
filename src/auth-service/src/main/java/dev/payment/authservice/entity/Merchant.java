package dev.payment.authservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(nullable = false)
    private String businessName;

    @Column
    private String webhookUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MerchantStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum MerchantStatus {
        ACTIVE,
        BLOCKED,
        PENDING_VERIFICATION
    }

    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (apiKey == null) {
            apiKey = "sk_test_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (status == null) {
            status = MerchantStatus.PENDING_VERIFICATION;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}