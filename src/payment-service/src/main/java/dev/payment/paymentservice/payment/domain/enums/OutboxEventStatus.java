package dev.payment.paymentservice.payment.domain.enums;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
