package dev.payment.paymentservice.service;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {
    private final PaymentRepository paymentRepository;
    private final OutboxPoller outboxPoller;

    @Value("${app.simulator.url}")
    private String simulatorUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void reconcile() {
        log.info("Starting reconciliation check");
        
        List<Payment> pendingPayments = paymentRepository.findByStatusIn(List.of(
            PaymentStatus.AUTHORIZED, 
            PaymentStatus.CAPTURED
        ));
        
        int drifts = 0;
        for (Payment payment : pendingPayments) {
            try {
                drifts += reconcilePayment(payment);
            } catch (Exception e) {
                log.error("Error reconciling payment {}: {}", payment.getId(), e.getMessage());
            }
        }
        
        log.info("Reconciliation complete. Fixed {} drifts", drifts);
    }

    private int reconcilePayment(Payment payment) {
        String providerRef = payment.getProviderReference();
        if (providerRef == null || providerRef.isBlank()) {
            return 0;
        }

        try {
            String url = simulatorUrl + "/simulate/status/" + providerRef;
            Map<String, Object> providerStatus = restTemplate.getForObject(url, Map.class);
            
            PaymentStatus localStatus = payment.getStatus();
            PaymentStatus remoteStatus = mapProviderStatus((String) providerStatus.get("status"));
            
            if (remoteStatus != null && localStatus != remoteStatus) {
                log.warn("Drift detected for payment {}: local={}, remote={}", 
                    payment.getId(), localStatus, remoteStatus);
                
                fixDrift(payment, remoteStatus);
                return 1;
            }
        } catch (Exception e) {
            log.error("Error fetching provider status for {}: {}", payment.getId(), e.getMessage());
        }
        
        return 0;
    }

    private void fixDrift(Payment payment, PaymentStatus correctStatus) {
        if (correctStatus == null) return;
        
        PaymentStatus fromStatus = payment.getStatus();
        payment.setStatus(correctStatus);
        payment.setUpdatedAt(Instant.now());
        paymentRepository.save(payment);
        
        outboxPoller.createEvent(payment.getId().toString(), "PAYMENT_RECONCILED", 
            Map.of(
                "paymentId", payment.getId(),
                "fromStatus", fromStatus.name(),
                "correctStatus", correctStatus.name()
            ));
        
        log.info("Fixed drift for payment {}: {} -> {}", payment.getId(), fromStatus, correctStatus);
    }

    private PaymentStatus mapProviderStatus(String providerStatus) {
        if (providerStatus == null) return null;
        return switch (providerStatus.toUpperCase()) {
            case "AUTHORIZED" -> PaymentStatus.AUTHORIZED;
            case "CAPTURED" -> PaymentStatus.CAPTURED;
            case "FAILED" -> PaymentStatus.FAILED;
            default -> null;
        };
    }
}