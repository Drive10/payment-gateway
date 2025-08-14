package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.service.LocalBankSimulator;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sim/bank")
@Profile("dev")
public class LocalBankSimulatorController {

    private final LocalBankSimulator sim;

    public LocalBankSimulatorController(LocalBankSimulator sim) { this.sim = sim; }

    @PostMapping("/status/{transactionId}")
    public ResponseEntity<Map<String,String>> setStatus(@PathVariable String transactionId,
                                                        @RequestParam String status) {
        boolean ok = sim.setStatus(transactionId, status);
        if (!ok) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("transactionId", transactionId, "status", status));
    }
}
