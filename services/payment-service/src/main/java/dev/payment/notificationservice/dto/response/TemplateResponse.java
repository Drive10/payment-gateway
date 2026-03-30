package dev.payment.notificationservice.dto.response;

import java.util.UUID;

public record TemplateResponse(UUID id, String templateCode, String channel, String subject, String body) {}
