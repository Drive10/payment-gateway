package dev.payment.paymentservice.order.entity;

public enum OrderStatus {
    PENDING,                   // Order created, awaiting payment initiation
    PAYMENT_PENDING,           // Payment initiated, awaiting completion
    PAID,                      // Payment successfully captured
    COMPLETED,                 // Order fulfilled/delivered
    FAILED,                    // Payment failed
    CANCELLED,                 // Order cancelled by user/merchant
    EXPIRED,                   // Payment not received within timeout
    REFUNDED                   // Payment was refunded
}
