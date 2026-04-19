package dev.payment.common.dto;

public enum OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    PAID,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
    REFUNDED
}