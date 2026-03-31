package dev.payment.common.saga;

import java.time.Instant;
import java.util.UUID;

public class SagaOrchestrator {

    private final String sagaId;
    private final SagaEvent.PaymentInitiated initialEvent;
    private SagaStatus status;

    public SagaOrchestrator(SagaEvent.PaymentInitiated event) {
        this.sagaId = UUID.randomUUID().toString();
        this.initialEvent = event;
        this.status = SagaStatus.STARTED;
    }

    public String getSagaId() {
        return sagaId;
    }

    public SagaStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStatus status) {
        this.status = status;
    }

    public SagaEvent.PaymentInitiated getInitialEvent() {
        return initialEvent;
    }

    public SagaEvent.RiskCheckCompleted handleRiskCheckCompleted(SagaEvent.RiskCheckCompleted event) {
        if (!event.approved()) {
            this.status = SagaStatus.PAYMENT_FAILED;
            return null;
        }
        this.status = SagaStatus.PAYMENT_PROCESSING;
        return event;
    }

    public SagaEvent.PaymentProcessed handlePaymentProcessed(SagaEvent.PaymentProcessed event) {
        this.status = SagaStatus.SETTLEMENT_IN_PROGRESS;
        return event;
    }

    public SagaEvent.SettlementCompleted handleSettlementCompleted(SagaEvent.SettlementCompleted event) {
        this.status = SagaStatus.SETTLEMENT_COMPLETED;
        return event;
    }

    public SagaEvent.RefundInitiated handleRefundInitiated(SagaEvent.RefundInitiated event) {
        this.status = SagaStatus.REFUND_IN_PROGRESS;
        return event;
    }

    public void handleCompensation(String reason) {
        this.status = SagaStatus.COMPENSATING;
    }
}
