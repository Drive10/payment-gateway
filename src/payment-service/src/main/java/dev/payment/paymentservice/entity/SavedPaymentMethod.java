package dev.payment.paymentservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saved_payment_methods", schema = "public", indexes = {
    @Index(name = "idx_spm_customer_email", columnList = "customer_email"),
    @Index(name = "idx_spm_token", columnList = "token", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedPaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String type;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "card_brand")
    private String cardBrand;

    @JsonIgnore
    @Column(name = "expiry_month")
    private Integer expiryMonth;

    @JsonIgnore
    @Column(name = "expiry_year")
    private Integer expiryYear;

    @JsonIgnore
    @Column(name = "cardholder_name")
    private String cardholderName;

    @JsonIgnore
    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "is_default")
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "is_expired")
    @Builder.Default
    private boolean isExpired = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (token == null)
            token = "pm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
