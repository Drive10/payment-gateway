package dev.payment.notificationservice.entity;

public enum NotificationType {
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    PAYMENT_PENDING,
    ORDER_CONFIRMATION,
    REFUND_PROCESSED,
    SUBSCRIPTION_RENEWED,
    SECURITY_ALERT,
    ACCOUNT_VERIFICATION
}
