package dev.payment.common.dto;

import java.math.BigDecimal;

public record OrderSnapshot(
    String id,
    BigDecimal amount,
    String currency
) {}