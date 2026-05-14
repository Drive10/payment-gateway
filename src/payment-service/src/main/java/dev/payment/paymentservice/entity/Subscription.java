package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", schema = "public", indexes = {
    @Index(name = "idx_sub_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_sub_customer_email", columnList = "customer_email"),
    @Index(name = "idx_sub_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false, unique = true)
    private String subscriptionId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "plan_name")
    private String planName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String interval;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Column(name = "payment_method_token")
    private String paymentMethodToken;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "trial_end")
    private Instant trialEnd;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "billing_cycles")
    private Integer billingCycles;

    @Column(name = "billing_cycles_done")
    @Builder.Default
    private Integer billingCyclesDone = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (subscriptionId == null)
            subscriptionId = "sub_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        if (status == null) status = SubscriptionStatus.ACTIVE;
        if (billingCyclesDone == null) billingCyclesDone = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum SubscriptionStatus {
        ACTIVE, PAUSED, PAST_DUE, CANCELLED, EXPIRED, TRIALING
    }
}
