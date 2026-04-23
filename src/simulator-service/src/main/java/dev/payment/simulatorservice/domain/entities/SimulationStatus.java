package dev.payment.simulatorservice.model;

public enum SimulationStatus {
    PENDING,
    PROCESSING,
    PENDING_3DS,
    CHALLENGE_REQUIRED,
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILED,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    CANCELLED
}