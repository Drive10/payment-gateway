package dev.payment.simulatorservice.model;

public enum SimulationMode {
    SUCCESS,
    FAILURE,
    INSUFFICIENT_FUNDS,
    CARD_DECLINED,
    TIMEOUT,
    TEST
}