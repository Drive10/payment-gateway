package dev.payment.auditservice.service;

import dev.payment.auditservice.entity.AuditLog;
import dev.payment.auditservice.repository.AuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    private AuditService auditService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditService = new AuditService(auditRepository, objectMapper);
    }

    @Test
    void handlePaymentEvent_ValidMessage_ShouldRecordAuditLog() {
        String validMessage = "{\"paymentId\":\"test-payment-123\",\"orderId\":\"order-456\",\"amount\":1000,\"currency\":\"USD\"}";

        when(auditRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        auditService.handlePaymentEvent(validMessage);

        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void handlePaymentEvent_DoubleEncodedMessage_ShouldRecordAuditLog() {
        String doubleEncodedMessage = "\"{\\\"paymentId\\\":\\\"test-payment-123\\\",\\\"orderId\\\":\\\"order-456\\\",\\\"amount\\\":1000,\\\"currency\\\":\\\"USD\\\"}\"";

        when(auditRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog a = invocation.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        auditService.handlePaymentEvent(doubleEncodedMessage);

        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void handlePaymentEvent_InvalidMessage_ShouldHandleGracefully() {
        String invalidMessage = "not valid json";

        auditService.handlePaymentEvent(invalidMessage);

        verify(auditRepository, never()).save(any(AuditLog.class));
    }
}