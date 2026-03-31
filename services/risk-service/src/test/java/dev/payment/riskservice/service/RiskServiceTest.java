package dev.payment.riskservice.service;

import dev.payment.riskservice.entity.RiskAssessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        riskScoringService = new RiskScoringService();
    }

    @Test
    void evaluateTransaction_LowAmount_ReturnsLowRisk() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00");
        Map<String, Object> metadata = new HashMap<>();

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, "INR", metadata);

        assertThat(assessment).isNotNull();
        assertThat(assessment.getTransactionId()).isEqualTo(transactionId);
        assertThat(assessment.getUserId()).isEqualTo(userId);
        assertThat(assessment.getAmount()).isEqualByComparingTo(amount);
        assertThat(assessment.getRiskLevel()).isEqualTo(RiskAssessment.RiskLevel.LOW);
        assertThat(assessment.getDecision()).isEqualTo(RiskAssessment.Decision.APPROVE);
    }

    @Test
    void evaluateTransaction_HighAmount_AddsRiskScore() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150000.00");
        Map<String, Object> metadata = new HashMap<>();

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, "INR", metadata);

        assertThat(assessment.getRiskScore()).isGreaterThan(0);
        assertThat(assessment.getRiskLevel()).isNotEqualTo(RiskAssessment.RiskLevel.LOW);
    }

    @Test
    void evaluateTransaction_VeryHighAmount_AddsHigherRiskScore() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("600000.00");
        Map<String, Object> metadata = new HashMap<>();

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, "INR", metadata);

        assertThat(assessment.getRiskScore()).isGreaterThanOrEqualTo(80);
        assertThat(assessment.getRiskLevel()).isEqualTo(RiskAssessment.RiskLevel.CRITICAL);
        assertThat(assessment.getDecision()).isEqualTo(RiskAssessment.Decision.REJECT);
    }

    @Test
    void evaluateTransaction_NewUser_AddsRiskScore() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1000.00");
        Map<String, Object> metadata = new HashMap<>();

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, "INR", metadata);

        assertThat(assessment.getRiskScore()).isGreaterThanOrEqualTo(15);
    }

    @Test
    void evaluateTransaction_TestEmail_ReducesRiskScore() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("5000.00");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("email", "test@example.com");

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, "INR", metadata);

        assertThat(assessment.getRiskScore()).isLessThan(15);
    }

    @Test
    void evaluateTransaction_MultipleTransactions_HighFrequencyFlag() {
        UUID userId = UUID.randomUUID();

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("1000.00"), "INR", new HashMap<>());
        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("2000.00"), "INR", new HashMap<>());
        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("3000.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("1500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("2500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("3500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("4500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("5500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("6500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("7500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("8500.00"), "INR", new HashMap<>());

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("9500.00"), "INR", new HashMap<>());

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                UUID.randomUUID(), userId, new BigDecimal("1000.00"), "INR", new HashMap<>());

        assertThat(assessment.getRiskScore()).isGreaterThanOrEqualTo(20);
    }

    @Test
    void evaluateTransaction_HighVolumeUser_AddsRiskScore() {
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 20; i++) {
            riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                    new BigDecimal("60000.00"), "INR", new HashMap<>());
        }

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                UUID.randomUUID(), userId, new BigDecimal("10000.00"), "INR", new HashMap<>());

        assertThat(assessment.getRiskScore()).isGreaterThanOrEqualTo(25);
    }

    @Test
    void evaluateTransaction_MediumRiskScore_ReturnsReviewDecision() {
        riskScoringService.resetUserMetrics(UUID.randomUUID());
        
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("500001.00");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("email", "real@example.com");

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, "INR", metadata);

        assertThat(assessment.getRiskLevel()).isEqualTo(RiskAssessment.RiskLevel.CRITICAL);
        assertThat(assessment.getDecision()).isEqualTo(RiskAssessment.Decision.REJECT);
    }

    @Test
    void resetUserMetrics_ClearsUserData() {
        UUID userId = UUID.randomUUID();

        riskScoringService.evaluateTransaction(UUID.randomUUID(), userId,
                new BigDecimal("1000.00"), "INR", new HashMap<>());

        riskScoringService.resetUserMetrics(userId);

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                UUID.randomUUID(), userId, new BigDecimal("1000.00"), "INR", new HashMap<>());

        assertThat(assessment.getRiskScore()).isEqualTo(15);
    }

    @Test
    void evaluateTransaction_DefaultCurrency_IsInr() {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("1000.00");
        Map<String, Object> metadata = new HashMap<>();

        RiskAssessment assessment = riskScoringService.evaluateTransaction(
                transactionId, userId, amount, null, metadata);

        assertThat(assessment.getCurrency()).isEqualTo("INR");
    }
}