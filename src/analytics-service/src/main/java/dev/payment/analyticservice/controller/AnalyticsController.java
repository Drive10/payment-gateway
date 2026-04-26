package dev.payment.analyticservice.controller;

import dev.payment.analyticservice.entity.SettlementMetric;
import dev.payment.analyticservice.repository.SettlementRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "API for retrieving analytics and settlement metrics")
public class AnalyticsController {
    private final SettlementRepository settlementRepository;

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "Get merchant metrics", description = "Retrieve settlement metrics for a merchant")
    public ResponseEntity<SettlementMetric> getMerchantMetrics(@PathVariable String merchantId) {
        return settlementRepository.findByMerchantId(merchantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
