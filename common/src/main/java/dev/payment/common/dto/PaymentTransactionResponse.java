package dev.payment.common.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionResponse(
    UUID id,
    String type,
    String status,
    BigDecimal amount,
    String providerReference,
    String remarks,
    Instant createdAt
) {}