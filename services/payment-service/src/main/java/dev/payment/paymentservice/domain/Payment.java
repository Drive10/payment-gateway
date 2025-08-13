package dev.payment.paymentservice.domain;

import java.math.BigDecimal;

public record Payment(
    String id,
    String orderId,
    String customerId,
    String currency,
    BigDecimal amount,
    String status) {}
