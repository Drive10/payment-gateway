package dev.payment.paymentservice.domain.enums;

public enum PaymentStatus {
    CREATED,
    PROCESSING,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED
}
