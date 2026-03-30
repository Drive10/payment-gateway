package dev.payment.simulatorservice.dto.request;

import dev.payment.simulatorservice.domain.enums.SimulationMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CaptureSimulationRequest(
        @NotBlank @Size(max = 64) String paymentReference,
        @NotNull SimulationMode simulationMode
) {
}
