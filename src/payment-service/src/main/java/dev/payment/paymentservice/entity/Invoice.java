package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invoices", schema = "public", indexes = {
    @Index(name = "idx_inv_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_inv_customer_email", columnList = "customer_email"),
    @Index(name = "idx_inv_status", columnList = "status"),
    @Index(name = "idx_inv_subscription_id", columnList = "subscription_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_name")
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "subscription_id")
    private String subscriptionId;

    @Column(columnDefinition = "TEXT")
    private String lineItems;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (invoiceNumber == null)
            invoiceNumber = "INV-" + Instant.now().getEpochSecond() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        if (status == null) status = InvoiceStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum InvoiceStatus {
        DRAFT, PENDING, PAID, OVERDUE, CANCELLED, REFUNDED
    }
}
