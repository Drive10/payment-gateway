package dev.payment.analyticservice.controller;

import dev.payment.analyticservice.entity.SettlementMetric;
import dev.payment.analyticservice.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final SettlementRepository settlementRepository;

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<SettlementMetric> getMerchantMetrics(@PathVariable String merchantId) {
        return settlementRepository.findByMerchantId(merchantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
