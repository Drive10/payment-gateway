package dev.payment.simulatorservice.dto.request;

import dev.payment.simulatorservice.model.SimulationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateSimulationRequest(
        @NotBlank @Size(max = 64) String orderReference,
        @NotBlank @Size(max = 64) String paymentReference,
        @NotBlank @Size(max = 32) String provider,
        @NotNull BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull SimulationMode simulationMode,
        @Size(max = 255) String notes,
        @Size(max = 512) String webhookCallbackUrl
) {
}