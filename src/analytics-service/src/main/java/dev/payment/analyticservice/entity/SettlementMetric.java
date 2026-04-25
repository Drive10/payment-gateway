package dev.payment.analyticservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_metrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String merchantId;

    @Column(nullable = false)
    private BigDecimal totalVolume;

    @Column(nullable = false)
    private long successfulPayments;

    @Column(nullable = false)
    private long failedPayments;

    @Column(nullable = false)
    private Instant lastUpdated;
}
