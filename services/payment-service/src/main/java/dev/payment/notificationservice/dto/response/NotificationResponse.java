package dev.payment.notificationservice.dto.response;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String recipientAddress, String templateCode, String channel, String status, String payload, Instant createdAt) {}
