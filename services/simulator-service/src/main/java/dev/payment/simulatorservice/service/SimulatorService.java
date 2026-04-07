package dev.payment.simulatorservice.service;

import dev.payment.simulatorservice.dto.request.CreateSimulationRequest;
import dev.payment.simulatorservice.dto.response.SimulationResponse;
import dev.payment.simulatorservice.model.SimulationMode;
import dev.payment.simulatorservice.model.SimulationStatus;
import dev.payment.simulatorservice.model.SimulationTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SimulatorService {

    private final Map<String, SimulationTransaction> transactions = new ConcurrentHashMap<>();

    @Value("${simulator.delay.ms:100}")
    private int delayMs;

    @Value("${simulator.success-rate:0.9}")
    private double successRate;

    public SimulationResponse createIntent(CreateSimulationRequest request) {
        simulateDelay();

        String transactionId = UUID.randomUUID().toString();
        String providerOrderId = "sim_" + System.currentTimeMillis();

        SimulationTransaction tx = SimulationTransaction.builder()
                .id(transactionId)
                .orderReference(request.orderReference())
                .paymentReference(request.paymentReference())
                .provider(request.provider().toUpperCase())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .simulationMode(request.simulationMode())
                .status(SimulationStatus.PENDING)
                .providerOrderId(providerOrderId)
                .checkoutUrl("https://simulator.payflow.dev/checkout/" + providerOrderId)
                .notes(request.notes())
                .webhookCallbackUrl(request.webhookCallbackUrl())
                .createdAt(Instant.now())
                .build();

        transactions.put(transactionId, tx);
        log.info("Created simulation transaction: {}", transactionId);

        return toResponse(tx);
    }

    public SimulationResponse capture(String providerOrderId) {
        simulateDelay();

        SimulationTransaction tx = transactions.values().stream()
                .filter(t -> providerOrderId.equals(t.getProviderOrderId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + providerOrderId));

        boolean success = shouldSucceed(tx.getSimulationMode());
        
        if (success) {
            tx.setStatus(SimulationStatus.CAPTURED);
            tx.setProviderPaymentId("pay_" + System.currentTimeMillis());
            log.info("Captured transaction: {}", providerOrderId);
        } else {
            tx.setStatus(SimulationStatus.FAILED);
            tx.setNotes("Simulated failure");
            log.warn("Captured failed for transaction: {}", providerOrderId);
        }

        return toResponse(tx);
    }

    public SimulationResponse getStatus(String providerOrderId) {
        SimulationTransaction tx = transactions.values().stream()
                .filter(t -> providerOrderId.equals(t.getProviderOrderId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + providerOrderId));

        return toResponse(tx);
    }

    public List<SimulationResponse> getTransactions(SimulationMode mode, SimulationStatus status) {
        return transactions.values().stream()
                .filter(t -> mode == null || t.getSimulationMode() == mode)
                .filter(t -> status == null || t.getStatus() == status)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private boolean shouldSucceed(SimulationMode mode) {
        if (mode == SimulationMode.SUCCESS || mode == SimulationMode.TEST) {
            return true;
        }
        if (mode == SimulationMode.FAILURE || mode == SimulationMode.CARD_DECLINED) {
            return false;
        }
        return Math.random() < successRate;
    }

    private void simulateDelay() {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SimulationResponse toResponse(SimulationTransaction tx) {
        return SimulationResponse.builder()
                .id(tx.getId())
                .orderReference(tx.getOrderReference())
                .paymentReference(tx.getPaymentReference())
                .provider(tx.getProvider())
                .providerOrderId(tx.getProviderOrderId())
                .providerPaymentId(tx.getProviderPaymentId())
                .status(tx.getStatus().name())
                .amount(tx.getAmount())
                .currency(tx.getCurrency())
                .checkoutUrl(tx.getCheckoutUrl())
                .testMode(tx.getSimulationMode() == SimulationMode.TEST)
                .notes(tx.getNotes())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}