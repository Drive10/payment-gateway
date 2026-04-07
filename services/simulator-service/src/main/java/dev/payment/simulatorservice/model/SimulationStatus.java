package dev.payment.simulatorservice.model;

public enum SimulationStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    CANCELLED
}