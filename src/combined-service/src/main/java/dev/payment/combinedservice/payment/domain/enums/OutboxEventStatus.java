package dev.payment.combinedservice.payment.domain.enums;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTER
}
