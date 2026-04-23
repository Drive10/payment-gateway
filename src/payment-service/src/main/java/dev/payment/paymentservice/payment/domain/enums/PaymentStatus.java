package dev.payment.paymentservice.payment.domain.enums;

public enum PaymentStatus {
    PENDING,                   // Initial state - payment created, awaiting initiation
    CREATED,                   // Payment initiated, awaiting authorization
    AWAITING_UPI_PAYMENT,      // UPI: Waiting for user to complete payment in UPI app
    AUTHORIZATION_PENDING,    // Awaiting user/bank verification (OTP, 3D secure)
    AUTHORIZED,                // Payment authorized, awaiting capture
    PROCESSING,                // Currently being processed by payment provider
    CAPTURED,                  // Money received - final success state
    PARTIALLY_REFUNDED,        // Partial refund issued
    REFUNDED,                  // Full refund issued
    FAILED,                    // Payment failed - final failure state
    EXPIRED                    // Payment expired - no action taken
}
