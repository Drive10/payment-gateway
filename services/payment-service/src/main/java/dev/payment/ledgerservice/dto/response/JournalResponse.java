package dev.payment.ledgerservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JournalResponse(UUID id, String reference, String debitAccountCode, String creditAccountCode, BigDecimal amount, String narration, Instant createdAt) {
}
