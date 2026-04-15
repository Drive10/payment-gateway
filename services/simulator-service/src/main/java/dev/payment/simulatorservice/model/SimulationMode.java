package dev.payment.simulatorservice.model;

public enum SimulationMode {
    SUCCESS,
    FAILURE,
    INSUFFICIENT_FUNDS,
    CARD_DECLINED,
    EXPIRED_CARD,
    INVALID_CARD,
    LOST_CARD,
    RISK_REJECTED,
    TIMEOUT,
    NETWORK_ERROR,
    CALLER_ERROR,
    TEST
}