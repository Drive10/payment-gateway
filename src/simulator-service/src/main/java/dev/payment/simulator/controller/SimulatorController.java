package dev.payment.simulator.controller;

import dev.payment.simulator.service.SimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
public class SimulatorController {
    private final SimulatorService simulatorService;

    @PostMapping("/payment/{paymentId}")
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