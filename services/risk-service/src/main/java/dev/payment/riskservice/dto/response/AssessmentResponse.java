package dev.payment.riskservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentResponse(UUID id, String merchantReference, BigDecimal amount, String countryCode, int velocityCount, int riskScore, String decision, String reasons, Instant createdAt) {}
