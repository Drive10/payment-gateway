package dev.payment.common.dto;

import java.time.Instant;
import java.util.UUID;

public record Merchant(
    UUID id,
    String apiKey,
    String status,
    String webhookUrl,
    Instant createdAt
) {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BLOCKED = "BLOCKED";

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }
}