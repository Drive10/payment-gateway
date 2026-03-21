package dev.payment.ledgerservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, String accountCode, String accountName, String type, BigDecimal balance, Instant createdAt) {
}
