package dev.payment.analyticservice.service;

import dev.payment.analyticservice.entity.SettlementMetric;
import dev.payment.analyticservice.repository.SettlementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyticsServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    private AnalyticsService analyticsService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        analyticsService = new AnalyticsService(settlementRepository, objectMapper);
    }

    @Test
    void handlePaymentEvent_ValidCapturedMessage_ShouldUpdateMetrics() {
        String validMessage = "{\"paymentId\":\"test-123\",\"merchantId\":\"merchant-456\",\"amount\":1000,\"currency\":\"USD\",\"status\":\"CAPTURED\"}";

        SettlementMetric existingMetric = SettlementMetric.builder()
                .merchantId("merchant-456")
                .totalVolume(BigDecimal.valueOf(5000))
                .successfulPayments(5L)
                .failedPayments(1L)
                .build();

        when(settlementRepository.findByMerchantId("merchant-456")).thenReturn(java.util.Optional.of(existingMetric));
        when(settlementRepository.save(any(SettlementMetric.class))).thenReturn(existingMetric);

        analyticsService.handlePaymentEvent(validMessage);

        verify(settlementRepository, times(1)).save(any(SettlementMetric.class));
    }

    @Test
    void handlePaymentEvent_ValidFailedMessage_ShouldUpdateMetrics() {
        String validMessage = "{\"paymentId\":\"test-123\",\"merchantId\":\"merchant-789\",\"amount\":1000,\"currency\":\"USD\",\"status\":\"FAILED\"}";

        SettlementMetric existingMetric = SettlementMetric.builder()
                .merchantId("merchant-789")
                .totalVolume(BigDecimal.valueOf(5000))
                .successfulPayments(5L)
                .failedPayments(1L)
                .build();

        when(settlementRepository.findByMerchantId("merchant-789")).thenReturn(java.util.Optional.of(existingMetric));
        when(settlementRepository.save(any(SettlementMetric.class))).thenReturn(existingMetric);

        analyticsService.handlePaymentEvent(validMessage);

        verify(settlementRepository, times(1)).save(any(SettlementMetric.class));
    }

    @Test
    void handlePaymentEvent_InvalidMessage_ShouldHandleGracefully() {
        String invalidMessage = "not valid json";

        analyticsService.handlePaymentEvent(invalidMessage);

        verify(settlementRepository, never()).save(any(SettlementMetric.class));
    }
}