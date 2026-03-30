package dev.payment.settlementservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SettlementBatchResponse(UUID id, String batchReference, BigDecimal totalAmount, String status, Instant createdAt) {}
