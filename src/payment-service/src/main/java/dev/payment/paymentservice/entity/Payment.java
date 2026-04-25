package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public boolean canTransitionTo(PaymentStatus target) {
        return target.isValidTransitionFrom(this.status);
    }

    public enum PaymentStatus {
        CREATED,
        AUTHORIZATION_PENDING,
        CHALLENGE_REQUIRED,
        AUTHORIZED,
        CAPTURED,
        FAILED,
        CANCELLED,
        REFUNDED;

        public boolean isValidTransitionFrom(PaymentStatus from) {
            return switch (this) {
                case AUTHORIZATION_PENDING -> from == CREATED;
                case CHALLENGE_REQUIRED -> from == AUTHORIZATION_PENDING;
                case AUTHORIZED -> from == AUTHORIZATION_PENDING || from == CHALLENGE_REQUIRED;
                case CAPTURED -> from == AUTHORIZED;
                case FAILED -> from == CREATED || from == AUTHORIZATION_PENDING || from == CHALLENGE_REQUIRED;
                case CANCELLED -> from == CREATED || from == AUTHORIZATION_PENDING || from == AUTHORIZED;
                case REFUNDED -> from == CAPTURED;
                default -> false;
            };
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = PaymentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}