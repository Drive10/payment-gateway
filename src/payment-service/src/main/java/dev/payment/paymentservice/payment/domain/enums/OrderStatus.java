package dev.payment.paymentservice.payment.domain.enums;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    REFUNDED,
    FAILED,
    CANCELLED
}
