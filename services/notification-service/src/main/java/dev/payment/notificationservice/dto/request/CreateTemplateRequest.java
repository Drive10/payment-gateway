package dev.payment.notificationservice.dto.request;

import dev.payment.notificationservice.domain.Channel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTemplateRequest(@NotBlank String templateCode, @NotNull Channel channel, @NotBlank String subject, @NotBlank String body) {}
