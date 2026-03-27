package dev.payment.paymentservice.domain.enums;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
