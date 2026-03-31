package dev.payment.settlementservice.controller;

import dev.payment.settlementservice.entity.Settlement;
import dev.payment.settlementservice.service.SettlementService;
import dev.payment.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/platform/settlement")
public class SettlementController {
    
    private final SettlementService settlementService;
    
    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<Settlement>> createSettlement(@RequestBody Map<String, Object> request) {
        validateCreateRequest(request);

        UUID merchantId = UUID.fromString(getStringRequired(request, "merchantId"));
        String merchantName = getStringRequired(request, "merchantName");
        
        Settlement settlement = settlementService.createSettlement(
            merchantId,
            merchantName,
            java.time.LocalDateTime.now().minusDays(1),
            java.time.LocalDateTime.now()
        );
        
        return ResponseEntity.ok(ApiResponse.success(settlement));
    }
    
    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<Settlement>> processSettlement(@PathVariable UUID id) {
        Settlement settlement = settlementService.processSettlement(id);
        return ResponseEntity.ok(ApiResponse.success(settlement));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Settlement>> getSettlement(@PathVariable UUID id) {
        return settlementService.getSettlement(id)
            .map(s -> ResponseEntity.ok(ApiResponse.success(s)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<Settlement>>> getMerchantSettlements(@PathVariable UUID merchantId) {
        List<Settlement> settlements = settlementService.getMerchantSettlements(merchantId);
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }
    
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Settlement>>> getPendingSettlements() {
        List<Settlement> settlements = settlementService.getPendingSettlements();
        return ResponseEntity.ok(ApiResponse.success(settlements));
    }
    
    @GetMapping("/merchant/{merchantId}/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMerchantBalance(@PathVariable UUID merchantId) {
        var ms = settlementService.getOrCreateMerchantSettlement(merchantId, "Merchant");
        Map<String, Object> balance = Map.of(
            "merchantId", ms.getMerchantId(),
            "currentBalance", ms.getCurrentBalance(),
            "pendingBalance", ms.getPendingBalance(),
            "totalSettled", ms.getTotalSettled()
        );
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "service", "settlement-service"
        );
        return ResponseEntity.ok(ApiResponse.success(health));
    }
    
    private void validateCreateRequest(Map<String, Object> request) {
        if (request.get("merchantId") == null) {
            throw new IllegalArgumentException("merchantId is required");
        }
        if (request.get("merchantName") == null) {
            throw new IllegalArgumentException("merchantName is required");
        }
    }
    
    private String getStringRequired(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value.toString();
    }
}
