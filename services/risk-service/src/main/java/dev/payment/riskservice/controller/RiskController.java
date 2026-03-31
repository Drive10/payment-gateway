package dev.payment.riskservice.controller;

import dev.payment.riskservice.entity.RiskAssessment;
import dev.payment.riskservice.service.RiskScoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/platform/risk")
public class RiskController {
    
    private final RiskScoringService riskScoringService;
    
    public RiskController(RiskScoringService riskScoringService) {
        this.riskScoringService = riskScoringService;
    }
    
    @PostMapping("/evaluate")
    public ResponseEntity<RiskAssessment> evaluateTransaction(@RequestBody Map<String, Object> request) {
        UUID transactionId = UUID.fromString((String) request.get("transactionId"));
        UUID userId = UUID.fromString((String) request.get("userId"));
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String currency = (String) request.getOrDefault("currency", "INR");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) request.getOrDefault("metadata", Map.of());
        
        RiskAssessment assessment = riskScoringService.evaluateTransaction(
            transactionId, userId, amount, currency, metadata
        );
        
        return ResponseEntity.ok(assessment);
    }
    
    @GetMapping("/user/{userId}/reset")
    public ResponseEntity<Map<String, String>> resetUserMetrics(@PathVariable UUID userId) {
        riskScoringService.resetUserMetrics(userId);
        return ResponseEntity.ok(Map.of("message", "User metrics reset successfully"));
    }
}
