package dev.payment.simulatorservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.simulatorservice.domain.enums.SimulationMode;
import dev.payment.simulatorservice.domain.enums.SimulationStatus;
import dev.payment.simulatorservice.dto.response.SimulationResponse;
import dev.payment.simulatorservice.service.SimulatorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/platform/simulator")
public class PlatformController {

    private final SimulatorService simulatorService;

    public PlatformController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @GetMapping("/status")
    public ApiResponse<String> status() {
        return ApiResponse.success("simulator-service transaction domain is operational");
    }

    @GetMapping("/transactions")
    public ApiResponse<List<SimulationResponse>> getTransactions(
            @RequestParam(required = false) SimulationMode mode,
            @RequestParam(required = false) SimulationStatus status
    ) {
        return ApiResponse.success(simulatorService.getTransactions(mode, status));
    }
}
