package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id")
    private String orderId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(nullable = false)
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

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "provider")
    private String provider;

    @Column(name = "provider_order_id")
    private String providerOrderId;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(name = "provider_signature")
    private String providerSignature;

    @Column(name = "simulated", nullable = false)
    @Builder.Default
    private Boolean simulated = false;

    @Column(name = "transaction_mode")
    private String transactionMode;

    @Column(name = "method")
    private String method;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "upi_link")
    private String upiLink;

    @Column(name = "notes")
    private String notes;

    @Column(name = "pricing_tier")
    private String pricingTier;

    @Column(name = "platform_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "gateway_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(name = "refunded_amount", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

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
                case CAPTURED -> from == AUTHORIZED || from == CREATED;
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
        if (simulated == null) {
            simulated = false;
        }
        if (platformFee == null) {
            platformFee = BigDecimal.ZERO;
        }
        if (gatewayFee == null) {
            gatewayFee = BigDecimal.ZERO;
        }
        if (refundAmount == null) {
            refundAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}