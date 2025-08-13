package dev.payment.common.dto;

import java.math.BigDecimal;

public record PaymentDTO(
    String paymentId, String orderId, String customerId, String currency, BigDecimal amount) {}
