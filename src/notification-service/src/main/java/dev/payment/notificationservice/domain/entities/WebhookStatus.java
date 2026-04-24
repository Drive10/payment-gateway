package dev.payment.notificationservice.domain.entities;

public enum WebhookStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED,
    PENDING_RETRY
}
