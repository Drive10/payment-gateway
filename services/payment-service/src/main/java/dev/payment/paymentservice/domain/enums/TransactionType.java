package dev.payment.paymentservice.domain.enums;

public enum TransactionType {
    PAYMENT_INITIATED,
    PAYMENT_CAPTURED,
    PAYMENT_FAILED,
    REFUND_REQUESTED,
    REFUND_COMPLETED,
    WEBHOOK_PROCESSED
}
