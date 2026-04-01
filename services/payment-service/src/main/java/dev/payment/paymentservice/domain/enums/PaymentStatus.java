package dev.payment.paymentservice.domain.enums;

public enum PaymentStatus {
    PENDING,
    CREATED,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED,
    EXPIRED
}
