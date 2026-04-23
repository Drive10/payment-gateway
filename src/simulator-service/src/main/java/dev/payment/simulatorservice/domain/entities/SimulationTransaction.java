package dev.payment.simulatorservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationTransaction {

    private String id;
    private String orderReference;
    private String paymentReference;
    private String provider;
    private String providerOrderId;
    private String providerPaymentId;
    private String providerSignature;
    private SimulationMode simulationMode;
    private SimulationStatus status;
    private String declineCode;
    private String declineReason;
    private BigDecimal amount;
    private String currency;
    private String checkoutUrl;
    private boolean testMode;
    private String notes;
    private Instant createdAt;
    private Instant processedAt;
    private String webhookCallbackUrl;
    private boolean requires3ds;
    private String threeDsChallengeUrl;
    private String threeDsTransactionId;
    private String threeDsStatus;
    private CardInfo cardInfo;
    private String riskScore;
    private boolean highRisk;
    private String velocityCheck;
    private String networkRef;
    private int retryCount;
}