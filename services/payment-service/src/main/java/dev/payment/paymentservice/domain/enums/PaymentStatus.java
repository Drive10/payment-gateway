package dev.payment.paymentservice.domain.enums;

public enum PaymentStatus {
    CREATED,
    AUTHORIZED,
    CAPTURED,
    PARTIALLY_REFUNDED,
    REFUNDED,
    FAILED
}
