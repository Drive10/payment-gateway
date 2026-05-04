package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.RefundRequest;
import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.entity.Refund;
import dev.payment.paymentservice.entity.Refund.RefundStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.RefundRepository;
import dev.payment.paymentservice.repository.LedgerEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefundService refundService;

    private Refund refund;
    private Payment payment;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        refundService = new RefundService(refundRepository, paymentRepository, ledgerEntryRepository, redisTemplate, new ObjectMapper());

        paymentId = UUID.randomUUID();
        payment = Payment.builder()
                .id(paymentId)
                .orderId("order_test_123")
                .amount(BigDecimal.valueOf(1000))
                .currency("USD")
                .status(PaymentStatus.CAPTURED)
                .build();

        refund = Refund.builder()
                .id(UUID.randomUUID())
                .paymentId(paymentId.toString())
                .refundId("ref_test123")
                .amount(BigDecimal.valueOf(500))
                .currency("USD")
                .refundedAmount(BigDecimal.valueOf(500))
                .status(RefundStatus.PENDING)
                .reason("Customer request")
                .build();
    }

    @Test
    void createRefund_FullRefund_ShouldSucceed() {
        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentId.toString());
        request.setReason("Full refund");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        var result = refundService.createRefund(request, "merchant_123");

        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    void createRefund_PartialRefund_ShouldSucceed() {
        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentId.toString());
        request.setAmount(BigDecimal.valueOf(500));
        request.setReason("Partial refund");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        var result = refundService.createRefund(request, "merchant_123");

        assertNotNull(result);
        assertEquals(new BigDecimal("500"), result.getAmount());
        verify(refundRepository, times(1)).save(any(Refund.class));
    }

    @Test
    void createRefund_PaymentNotFound_ShouldThrowException() {
        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentId.toString());
        request.setReason("Test refund");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            refundService.createRefund(request, "merchant_123"));
    }

    @Test
    void getRefund_ExistingRefund_ShouldReturnRefund() {
        when(refundRepository.findById(refund.getId())).thenReturn(Optional.of(refund));

        var result = refundService.getRefund(refund.getId().toString());

        assertNotNull(result);
        assertEquals(refund.getId().toString(), result.getRefundId());
    }

    @Test
    void getRefund_NonExistingRefund_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        when(refundRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            refundService.getRefund(nonExistingId.toString()));
    }

    @Test
    void getRefundsByPaymentId_ShouldReturnRefund() {
        when(refundRepository.findByPaymentId(paymentId.toString())).thenReturn(Optional.of(refund));

        Optional<Refund> result = refundService.getRefundsByPaymentId(paymentId.toString());

        assertTrue(result.isPresent());
        assertEquals(refund.getId(), result.get().getId());
    }
}
