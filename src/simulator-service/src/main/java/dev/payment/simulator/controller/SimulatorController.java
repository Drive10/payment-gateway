package dev.payment.simulator.controller;

import dev.payment.simulator.service.SimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
@Tag(name = "Payment Simulator", description = "API for simulating payment outcomes")
public class SimulatorController {
    private final SimulatorService simulatorService;

    @PostMapping("/payment/{paymentId}")
    @Operation(summary = "Simulate payment", description = "Simulate a payment result for testing")
    public ResponseEntity<Map<String, Object>> simulatePayment(@PathVariable String paymentId) {
        String result = simulatorService.simulatePayment(paymentId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of(
                "paymentId", paymentId,
                "result", result
            )
        ));
    }
}