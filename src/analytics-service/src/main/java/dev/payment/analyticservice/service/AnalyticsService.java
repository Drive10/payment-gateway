package dev.payment.analyticservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.analyticservice.entity.SettlementMetric;
import dev.payment.analyticservice.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final SettlementRepository settlementRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"payment.payment_created", "payment.payment_status_updated"}, groupId = "analytics-group")
    public void handlePaymentEvent(String message) {
        try {
            Map<String, Object> eventData = objectMapper.readValue(message, Map.class);
            String merchantId = (String) eventData.get("merchantId");
            String status = (String) eventData.get("status");
            BigDecimal amount = new BigDecimal(eventData.get("amount").toString());

            updateMetrics(merchantId, status, amount);
        } catch (Exception e) {
            log.error("Error updating analytics: {}", e.getMessage());
        }
    }

    private void updateMetrics(String merchantId, String status, BigDecimal amount) {
        SettlementMetric metric = settlementRepository.findByMerchantId(merchantId)
            .orElse(SettlementMetric.builder()
                .merchantId(merchantId)
                .totalVolume(BigDecimal.ZERO)
                .successfulPayments(0L)
                .failedPayments(0L)
                .build());

        if ("CAPTURED".equals(status)) {
            metric.setTotalVolume(metric.getTotalVolume().add(amount));
            metric.setSuccessfulPayments(metric.getSuccessfulPayments() + 1);
        } else if ("FAILED".equals(status)) {
            metric.setFailedPayments(metric.getFailedPayments() + 1);
        }

        metric.setLastUpdated(Instant.now());
        settlementRepository.save(metric);
        log.info("Updated analytics for merchant: {}", merchantId);
    }
}
