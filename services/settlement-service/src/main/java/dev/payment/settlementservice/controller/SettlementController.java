package dev.payment.settlementservice.controller;

import dev.payment.settlementservice.entity.Settlement;
import dev.payment.settlementservice.service.SettlementService;
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
    public ResponseEntity<Settlement> createSettlement(@RequestBody Map<String, Object> request) {
        UUID merchantId = UUID.fromString((String) request.get("merchantId"));
        String merchantName = (String) request.get("merchantName");
        
        Settlement settlement = settlementService.createSettlement(
            merchantId,
            merchantName,
            java.time.LocalDateTime.now().minusDays(1),
            java.time.LocalDateTime.now()
        );
        
        return ResponseEntity.ok(settlement);
    }
    
    @PostMapping("/{id}/process")
    public ResponseEntity<Settlement> processSettlement(@PathVariable UUID id) {
        Settlement settlement = settlementService.processSettlement(id);
        return ResponseEntity.ok(settlement);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Settlement> getSettlement(@PathVariable UUID id) {
        return settlementService.getSettlement(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<List<Settlement>> getMerchantSettlements(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(settlementService.getMerchantSettlements(merchantId));
    }
    
    @GetMapping("/pending")
    public ResponseEntity<List<Settlement>> getPendingSettlements() {
        return ResponseEntity.ok(settlementService.getPendingSettlements());
    }
    
    @GetMapping("/merchant/{merchantId}/balance")
    public ResponseEntity<Map<String, Object>> getMerchantBalance(@PathVariable UUID merchantId) {
        var ms = settlementService.getOrCreateMerchantSettlement(merchantId, "Merchant");
        return ResponseEntity.ok(Map.of(
            "merchantId", ms.getMerchantId(),
            "currentBalance", ms.getCurrentBalance(),
            "pendingBalance", ms.getPendingBalance(),
            "totalSettled", ms.getTotalSettled()
        ));
    }
}
