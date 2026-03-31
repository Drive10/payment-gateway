package dev.payment.riskservice.controller;

import dev.payment.riskservice.entity.RiskAssessment;
import dev.payment.riskservice.service.RiskScoringService;
import dev.payment.common.api.ApiResponse;
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
    public ResponseEntity<ApiResponse<RiskAssessment>> evaluateTransaction(@RequestBody Map<String, Object> request) {
        validateEvaluateRequest(request);

        UUID transactionId = UUID.fromString(getStringRequired(request, "transactionId"));
        UUID userId = UUID.fromString(getStringRequired(request, "userId"));
        BigDecimal amount = new BigDecimal(getStringRequired(request, "amount"));
        String currency = getStringOrDefault(request, "currency", "INR");
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = request.get("metadata") instanceof Map 
                ? (Map<String, Object>) request.get("metadata") 
                : Map.of();
        
        RiskAssessment assessment = riskScoringService.evaluateTransaction(
            transactionId, userId, amount, currency, metadata
        );
        
        return ResponseEntity.ok(ApiResponse.success(assessment));
    }
    
    @GetMapping("/user/{userId}/reset")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetUserMetrics(@PathVariable UUID userId) {
        riskScoringService.resetUserMetrics(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "User metrics reset successfully")));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "risk-service"
        );
        return ResponseEntity.ok(ApiResponse.success(health));
    }
    
    private void validateEvaluateRequest(Map<String, Object> request) {
        if (request.get("transactionId") == null) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (request.get("userId") == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (request.get("amount") == null) {
            throw new IllegalArgumentException("amount is required");
        }
    }
    
    private String getStringRequired(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString();
    }
    
    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
