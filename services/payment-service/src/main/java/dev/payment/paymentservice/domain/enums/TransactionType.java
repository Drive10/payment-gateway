package dev.payment.paymentservice.domain.enums;

public enum TransactionType {
    PAYMENT,
    PAYMENT_INITIATED,
    PAYMENT_CAPTURED,
    REFUND,
    REFUND_REQUESTED,
    REFUND_COMPLETED,
    SETTLEMENT,
    FEE,
    PAYOUT,
    ADJUSTMENT
}
