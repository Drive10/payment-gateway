package dev.payment.notificationservice.entity;

public enum WebhookStatus {
    RECEIVED,
    PROCESSING,
    PROCESSED,
    FAILED,
    PENDING_RETRY
}
