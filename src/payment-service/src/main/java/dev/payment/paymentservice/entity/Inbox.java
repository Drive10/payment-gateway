package dev.payment.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inbox {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String messageId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant receivedAt;

    @Column
    private Instant processedAt;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int maxRetries = 3;

    @Column
    private Instant nextRetryAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean deadLettered = false;

    @Column
    private Instant deadLetteredAt;

    @Column
    private String deadLetterReason;

    public boolean isProcessed() {
        return processedAt != null;
    }

    public boolean canRetry() {
        return retryCount < maxRetries && !deadLettered;
    }

    public void incrementRetry() {
        this.retryCount++;
        this.nextRetryAt = calculateNextRetry();
    }

    private Instant calculateNextRetry() {
        long delay = (long) Math.pow(2, retryCount) * 1000;
        return Instant.now().plusMillis(delay);
    }
}