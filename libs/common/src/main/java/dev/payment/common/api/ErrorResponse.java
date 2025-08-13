package dev.payment.common.api;

public record ErrorResponse(String code, String message, String traceId) {}
