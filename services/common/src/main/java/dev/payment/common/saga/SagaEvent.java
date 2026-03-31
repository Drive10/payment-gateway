package dev.payment.common.saga;

import java.time.Instant;

public sealed interface SagaEvent permits 
        SagaEvent.PaymentInitiated,
        SagaEvent.RiskCheckCompleted,
        SagaEvent.PaymentProcessed,
        SagaEvent.PaymentFailed,
        SagaEvent.SettlementCompleted,
        SagaEvent.RefundInitiated,
        SagaEvent.RefundCompleted {

    record PaymentInitiated(
            String sagaId,
            String paymentId,
            String orderId,
            String userId,
            String merchantId,
            long amount,
            String currency,
            Instant timestamp
    ) implements SagaEvent {}

    record RiskCheckCompleted(
            String sagaId,
            String paymentId,
            boolean approved,
            String riskScore,
            Instant timestamp
    ) implements SagaEvent {}

    record PaymentProcessed(
            String sagaId,
            String paymentId,
            String transactionId,
            Instant timestamp
    ) implements SagaEvent {}

    record PaymentFailed(
            String sagaId,
            String paymentId,
            String reason,
            Instant timestamp
    ) implements SagaEvent {}

    record SettlementCompleted(
            String sagaId,
            String paymentId,
            String settlementId,
            Instant timestamp
    ) implements SagaEvent {}

    record RefundInitiated(
            String sagaId,
            String paymentId,
            String originalTransactionId,
            long amount,
            Instant timestamp
    ) implements SagaEvent {}

    record RefundCompleted(
            String sagaId,
            String paymentId,
            String refundTransactionId,
            Instant timestamp
    ) implements SagaEvent {}
}
