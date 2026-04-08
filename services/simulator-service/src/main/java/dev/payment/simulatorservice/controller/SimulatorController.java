package dev.payment.simulatorservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.simulatorservice.dto.request.CreateSimulationRequest;
import dev.payment.simulatorservice.dto.response.SimulationResponse;
import dev.payment.simulatorservice.service.SimulatorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/simulator/payments")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/intents")
    public ApiResponse<SimulationResponse> createIntent(@Valid @RequestBody CreateSimulationRequest request) {
        return ApiResponse.success(simulatorService.createIntent(request));
    }

    @PostMapping("/{providerOrderId}/capture")
    public ApiResponse<SimulationResponse> capture(@PathVariable("providerOrderId") String providerOrderId) {
        return ApiResponse.success(simulatorService.capture(providerOrderId));
    }

    @GetMapping("/{providerOrderId}")
    public ApiResponse<SimulationResponse> getTransaction(@PathVariable("providerOrderId") String providerOrderId) {
        return ApiResponse.success(simulatorService.getStatus(providerOrderId));
    }
}
