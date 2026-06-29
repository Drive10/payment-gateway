package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.dto.RefundRequest;
import dev.payment.paymentservice.dto.RefundResponse;
import dev.payment.paymentservice.entity.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.entity.Refund.RefundStatus;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RefundServiceTest {

    private RefundRepository refundRepository;
    private PaymentRepository paymentRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RefundService refundService;

    @BeforeEach
    void setUp() {
        refundRepository = mock(RefundRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);

        objectMapper = new ObjectMapper();
        refundService = new RefundService(refundRepository, paymentRepository, ledgerEntryRepository, redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createRefund_ShouldRefundCapturedPayment() {
        String merchantId = "merchant-123";
        UUID paymentUuid = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .orderId("order-1")
                .amount(new BigDecimal("10000"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .merchantId(merchantId)
                .refundAmount(BigDecimal.ZERO)
                .build();

        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentUuid.toString());
        request.setAmount(new BigDecimal("5000"));
        request.setReason("Customer request");

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(ledgerEntryRepository.existsByReference(anyString())).thenReturn(false);

        RefundResponse response = refundService.createRefund(request, merchantId, "idem-1");

        assertNotNull(response);
        assertEquals(new BigDecimal("5000"), response.getAmount());
        assertEquals(RefundStatus.PENDING.name(), response.getStatus());
        verify(refundRepository, times(1)).save(any(Refund.class));
        verify(ledgerEntryRepository, times(2)).save(any(LedgerEntry.class));
    }

    @Test
    void createRefund_WithNonCapturedPayment_ShouldThrowException() {
        RefundRequest request = new RefundRequest();
        request.setPaymentId(UUID.randomUUID().toString());
        request.setAmount(new BigDecimal("1000"));

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .status(PaymentStatus.CREATED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findById(any(UUID.class))).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> refundService.createRefund(request, "merchant-123", null));
    }

    @Test
    void createRefund_WithAmountExceedingBalance_ShouldThrowException() {
        UUID paymentUuid = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CAPTURED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .refundAmount(new BigDecimal("4000"))
                .build();

        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentUuid.toString());
        request.setAmount(new BigDecimal("2000"));

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> refundService.createRefund(request, "merchant-123", null));
    }

    @Test
    void createRefund_FullAmount_ShouldMarkPaymentRefunded() {
        UUID paymentUuid = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .orderId("order-1")
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .status(PaymentStatus.CAPTURED)
                .merchantId("merchant-123")
                .refundAmount(BigDecimal.ZERO)
                .build();

        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentUuid.toString());
        request.setAmount(new BigDecimal("5000"));
        request.setReason("Full refund");

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));
        when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> {
            Refund r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(ledgerEntryRepository.existsByReference(anyString())).thenReturn(false);

        refundService.createRefund(request, "merchant-123", null);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
    }

    @Test
    void getRefund_ShouldReturnRefundResponse() {
        UUID refundId = UUID.randomUUID();
        Refund refund = Refund.builder()
                .id(refundId)
                .paymentId("payment-1")
                .amount(new BigDecimal("1000"))
                .refundedAmount(new BigDecimal("1000"))
                .currency("INR")
                .status(RefundStatus.COMPLETED)
                .reason("Refund")
                .build();

        when(refundRepository.findById(refundId)).thenReturn(Optional.of(refund));

        RefundResponse response = refundService.getRefund(refundId.toString());

        assertNotNull(response);
        assertEquals(refundId.toString(), response.getRefundId());
        assertEquals(RefundStatus.COMPLETED.name(), response.getStatus());
    }

    @Test
    void getRefund_WithNonExistentRefund_ShouldThrowException() {
        when(refundRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> refundService.getRefund(UUID.randomUUID().toString()));
    }

    @Test
    void createRefund_WithZeroAmount_ShouldThrowException() {
        UUID paymentUuid = UUID.randomUUID();

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CAPTURED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .refundAmount(BigDecimal.ZERO)
                .build();

        RefundRequest request = new RefundRequest();
        request.setPaymentId(paymentUuid.toString());
        request.setAmount(BigDecimal.ZERO);

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> refundService.createRefund(request, "merchant-123", null));
    }
}
