package dev.payment.paymentservice.domain;

import dev.payment.paymentservice.domain.enums.WebhookStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries")
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WebhookStatus status;

    @Column(nullable = false)
    private Integer attemptCount;

    private LocalDateTime nextAttemptAt;

    private LocalDateTime deliveredAt;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastAttemptAt;
}
