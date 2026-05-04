package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_inbox_events", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookInboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "webhook_id", nullable = false, unique = true)
    private String webhookId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
