package dev.payment.simulatorservice.service;

import dev.payment.simulatorservice.dto.request.CreateSimulationRequest;
import dev.payment.simulatorservice.dto.response.SimulationResponse;
import dev.payment.simulatorservice.model.CardInfo;
import dev.payment.simulatorservice.model.SimulationMode;
import dev.payment.simulatorservice.model.SimulationStatus;
import dev.payment.simulatorservice.model.SimulationTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
    private final Map<String, List<String>> velocityCache = new ConcurrentHashMap<>();

    @Value("${simulator.delay.ms:100}")
    private int delayMs;

    @Value("${simulator.success-rate:0.9}")
    private double successRate;

    @Value("${simulator.risk.threshold:70}")
    private int riskThreshold;

    public SimulationResponse createIntent(CreateSimulationRequest request) {
        simulateDelay();

        String transactionId = UUID.randomUUID().toString();
        String providerOrderId = "sim_" + System.currentTimeMillis();

        CardInfo cardInfo = request.cardInfo();
        RiskResult riskResult = evaluateRisk(request);

        boolean requires3ds = cardInfo != null && cardInfo.isRequires3ds();

        if (riskResult.blocked()) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, 
                "Payment blocked: " + riskResult.reason());
        }

        SimulationStatus initialStatus = SimulationStatus.PENDING;
        String threeDsChallengeUrl = null;
        String threeDsTransactionId = null;

        if (requires3ds || riskResult.challengeRequired()) {
            initialStatus = SimulationStatus.CHALLENGE_REQUIRED;
            threeDsChallengeUrl = "https://simulator.payflow.dev/3ds/challenge/" + providerOrderId;
            threeDsTransactionId = "3ds_" + System.currentTimeMillis();
        }

        SimulationTransaction tx = SimulationTransaction.builder()
                .id(transactionId)
                .orderReference(request.orderReference())
                .paymentReference(request.paymentReference())
                .provider(request.provider().toUpperCase())
                .amount(request.amount())
                .currency(request.currency().toUpperCase())
                .simulationMode(request.simulationMode())
                .status(initialStatus)
                .providerOrderId(providerOrderId)
                .checkoutUrl("https://simulator.payflow.dev/checkout/" + providerOrderId)
                .notes(request.notes())
                .webhookCallbackUrl(request.webhookCallbackUrl())
                .createdAt(Instant.now())
                .cardInfo(cardInfo)
                .requires3ds(requires3ds)
                .threeDsChallengeUrl(threeDsChallengeUrl)
                .threeDsTransactionId(threeDsTransactionId)
                .riskScore(riskResult.score() + "")
                .highRisk(riskResult.score() >= riskThreshold)
                .velocityCheck(riskResult.velocityFlag())
                .networkRef(generateNetworkRef())
                .build();

        transactions.put(providerOrderId, tx);
        updateVelocityCache(request.paymentReference(), providerOrderId);

        log.info("Created transaction: {} mode={} risk={} 3ds={} networkRef={}", 
            providerOrderId, request.simulationMode(), riskResult.score(), requires3ds, tx.getNetworkRef());

        return toResponse(tx);
    }

    public SimulationResponse capture(String providerOrderId) {
        simulateDelay();

        SimulationTransaction tx = findTransaction(providerOrderId);
        ProcessResult result = processTransaction(tx);

        tx.setProcessedAt(Instant.now());

        if (result.success()) {
            tx.setStatus(SimulationStatus.CAPTURED);
            tx.setProviderPaymentId("pay_" + System.currentTimeMillis());
            tx.setProviderSignature(generateSignature(tx));
            tx.setDeclineCode(null);
            tx.setDeclineReason(null);
            log.info("Captured: {} paymentId={} authCode={}", 
                providerOrderId, tx.getProviderPaymentId(), tx.getProviderSignature());
        } else {
            tx.setStatus(SimulationStatus.FAILED);
            tx.setDeclineCode(result.declineCode());
            tx.setDeclineReason(result.declineReason());
            log.warn("Failed: {} code={} reason={}", providerOrderId, result.declineCode(), result.declineReason());
        }

        return toResponse(tx);
    }

    public SimulationResponse verify3ds(String providerOrderId, String authenticationStatus, String cardInfo) {
        SimulationTransaction tx = findTransaction(providerOrderId);

        if (tx.getStatus() != SimulationStatus.CHALLENGE_REQUIRED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "3DS not required");
        }

        String threeDsStatus;
        if ("SUCCESS".equals(authenticationStatus)) {
            tx.setStatus(SimulationStatus.AUTHENTICATION_SUCCESS);
            tx.setThreeDsStatus("Y");
            threeDsStatus = "Y";
            log.info("3DS success: {}", providerOrderId);
        } else if ("FAILED".equals(authenticationStatus)) {
            tx.setStatus(SimulationStatus.AUTHENTICATION_FAILED);
            tx.setThreeDsStatus("N");
            threeDsStatus = "N";
            log.warn("3DS failed: {}", providerOrderId);
        } else {
            tx.setThreeDsStatus("A");
            threeDsStatus = "A";
        }

        return toResponse(tx);
    }

    public SimulationResponse processOtp(String providerOrderId, String otp) {
        SimulationTransaction tx = findTransaction(providerOrderId);

        if (tx.getStatus() != SimulationStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP not required");
        }

        validateOtp(otp, tx.getCardInfo());

        tx.setStatus(SimulationStatus.CAPTURED);
        tx.setProviderPaymentId("pay_" + System.currentTimeMillis());
        tx.setProviderSignature(generateSignature(tx));

        log.info("OTP verified: {} paymentId={}", providerOrderId, tx.getProviderPaymentId());

        return toResponse(tx);
    }

    public SimulationResponse getStatus(String providerOrderId) {
        SimulationTransaction tx = findTransaction(providerOrderId);
        return toResponse(tx);
    }

    public List<SimulationResponse> getTransactions(SimulationMode mode, SimulationStatus status) {
        return transactions.values().stream()
                .filter(t -> mode == null || t.getSimulationMode() == mode)
                .filter(t -> status == null || t.getStatus() == status)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private SimulationTransaction findTransaction(String providerOrderId) {
        return transactions.get(providerOrderId);
    }

    private RiskResult evaluateRisk(CreateSimulationRequest request) {
        int riskScore = 0;
        String velocityFlag = null;
        boolean challengeRequired = false;
        String reason = null;

        if (request.amount().compareTo(new BigDecimal("10000")) > 0) {
            riskScore += 20;
            if (request.amount().compareTo(new BigDecimal("50000")) > 0) {
                riskScore += 30;
                challengeRequired = true;
            }
        }

        CardInfo card = request.cardInfo();
        if (card != null) {
            if ("AMEX".equals(card.getBrand())) {
                riskScore += 10;
            }
            if ("INTERNATIONAL".equals(card.getCountry()) || card.getCountry() == null) {
                riskScore += 15;
            }
            if ("PLATINUM".equals(card.getCardLevel()) || "GOLD".equals(card.getCardLevel())) {
                riskScore += 5;
            }
        }

        String paymentId = request.paymentReference();
        if (paymentId != null) {
            List<String> recent = velocityCache.get(paymentId);
            if (recent != null && recent.size() > 5) {
                riskScore += 25;
                velocityFlag = "HIGH_VELOCITY";
            }

            if (recent != null) {
                int failures = (int) recent.stream()
                        .map(transactions::get)
                        .filter(t -> t != null && t.getStatus() == SimulationStatus.FAILED)
                        .count();
                if (failures >= 3) {
                    riskScore += 30;
                    velocityFlag = (velocityFlag != null ? velocityFlag + "," : "") + "RETRY_ABUSE";
                }
            }
        }

        if (riskScore >= riskThreshold) {
            reason = "HIGH_RISK_SCORE";
        }

        return new RiskResult(riskScore, reason, challengeRequired, velocityFlag);
    }

    private ProcessResult processTransaction(SimulationTransaction tx) {
        SimulationMode mode = tx.getSimulationMode();

        if (mode == null) {
            mode = SimulationMode.SUCCESS;
        }

        switch (mode) {
            case SUCCESS:
            case TEST:
                return new ProcessResult(true, null, null);

            case FAILURE:
                return new ProcessResult(false, "generic_decline", "Transaction declined by issuer");

            case CARD_DECLINED:
                return new ProcessResult(false, "insufficient_funds", "Insufficient funds");

            case INSUFFICIENT_FUNDS:
                return new ProcessResult(false, "insufficient_funds", "Insufficient funds");

            case EXPIRED_CARD:
                return new ProcessResult(false, "expired_card", "Card has expired");

            case INVALID_CARD:
                return new ProcessResult(false, "invalid_card", "Invalid card number");

            case LOST_CARD:
                return new ProcessResult(false, "card_lost", "Card reported lost or stolen");

            case RISK_REJECTED:
                return new ProcessResult(false, "risk_rejected", "Transaction flagged by risk engine");

            case TIMEOUT:
                return new ProcessResult(false, "timeout", "Issuer did not respond in time");

            case NETWORK_ERROR:
                return new ProcessResult(false, "network_error", "Network communication error");

            case CALLER_ERROR:
                return new ProcessResult(false, "caller_error", "Invalid merchant configuration");

            default:
                return new ProcessResult(false, "unknown", "Unknown error");
        }
    }

    private void validateOtp(String otp, CardInfo card) {
        if (otp == null || otp.length() != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP format");
        }

        if (!"123456".equals(otp)) {
            String lastFour = card != null ? card.getLastFour() : "****";
            int simulatedFail = (int) (System.currentTimeMillis() % 10);
            if (simulatedFail < 2) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP for card ending " + lastFour);
            }
        }
    }

    private void updateVelocityCache(String paymentRef, String transactionId) {
        velocityCache.computeIfAbsent(paymentRef, k -> new java.util.ArrayList<>()).add(transactionId);
    }

    private String generateNetworkRef() {
        return "NR" + System.currentTimeMillis() + "" + (int)(Math.random() * 1000);
    }

    private String generateSignature(SimulationTransaction tx) {
        return "sig_" + tx.getProviderPaymentId() + "_" + tx.getAmount() + "_" + tx.getCurrency();
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
                .requires3ds(tx.isRequires3ds())
                .threeDsChallengeUrl(tx.getThreeDsChallengeUrl())
                .threeDsTransactionId(tx.getThreeDsTransactionId())
                .declineCode(tx.getDeclineCode())
                .declineReason(tx.getDeclineReason())
                .riskScore(tx.getRiskScore())
                .networkRef(tx.getNetworkRef())
                .build();
    }

    private record RiskResult(int score, String reason, boolean challengeRequired, String velocityFlag) {
        public boolean blocked() {
            return score >= 80;
        }
    }

    private record ProcessResult(boolean success, String declineCode, String declineReason) {}
}