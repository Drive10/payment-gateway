package dev.payment.riskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.api.ApiResponse;
import dev.payment.riskservice.entity.RiskAssessment;
import dev.payment.riskservice.service.RiskScoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RiskControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RiskScoringService riskScoringService;

    @InjectMocks
    private RiskController riskController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(riskController)
                .setControllerAdvice(new dev.payment.riskservice.exception.RiskExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void evaluateTransaction_ValidRequest_ReturnsOk() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RiskAssessment assessment = new RiskAssessment();
        assessment.setTransactionId(transactionId);
        assessment.setUserId(userId);
        assessment.setAmount(new BigDecimal("5000.00"));
        assessment.setRiskScore(10);
        assessment.setRiskLevel(RiskAssessment.RiskLevel.LOW);
        assessment.setDecision(RiskAssessment.Decision.APPROVE);

        when(riskScoringService.evaluateTransaction(eq(transactionId), eq(userId), any(), eq("INR"), any()))
                .thenReturn(assessment);

        Map<String, Object> request = new HashMap<>();
        request.put("transactionId", transactionId.toString());
        request.put("userId", userId.toString());
        request.put("amount", "5000.00");
        request.put("currency", "INR");

        mockMvc.perform(post("/internal/platform/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.data.decision").value("APPROVE"));
    }

    @Test
    void evaluateTransaction_MissingTransactionId_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", UUID.randomUUID().toString());
        request.put("amount", "5000.00");

        mockMvc.perform(post("/internal/platform/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateTransaction_MissingUserId_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("transactionId", UUID.randomUUID().toString());
        request.put("amount", "5000.00");

        mockMvc.perform(post("/internal/platform/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateTransaction_MissingAmount_ReturnsBadRequest() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("transactionId", UUID.randomUUID().toString());
        request.put("userId", UUID.randomUUID().toString());

        mockMvc.perform(post("/internal/platform/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evaluateTransaction_WithMetadata_ReturnsOk() throws Exception {
        UUID transactionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RiskAssessment assessment = new RiskAssessment();
        assessment.setTransactionId(transactionId);
        assessment.setUserId(userId);
        assessment.setAmount(new BigDecimal("1000.00"));
        assessment.setRiskScore(15);
        assessment.setRiskLevel(RiskAssessment.RiskLevel.LOW);
        assessment.setDecision(RiskAssessment.Decision.APPROVE);

        when(riskScoringService.evaluateTransaction(any(), any(), any(), any(), any()))
                .thenReturn(assessment);

        Map<String, Object> request = new HashMap<>();
        request.put("transactionId", transactionId.toString());
        request.put("userId", userId.toString());
        request.put("amount", "1000.00");
        request.put("currency", "USD");
        request.put("metadata", Map.of("email", "test@example.com", "ip", "192.168.1.1"));

        mockMvc.perform(post("/internal/platform/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resetUserMetrics_ValidUserId_ReturnsOk() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/internal/platform/risk/user/{userId}/reset", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.message").value("User metrics reset successfully"));
    }

    @Test
    @Disabled("Health endpoint now uses Spring Actuator")
    void health_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}