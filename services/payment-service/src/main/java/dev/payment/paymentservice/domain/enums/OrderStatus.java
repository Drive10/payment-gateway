package dev.payment.paymentservice.domain.enums;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    REFUNDED,
    FAILED,
    CANCELLED
}
