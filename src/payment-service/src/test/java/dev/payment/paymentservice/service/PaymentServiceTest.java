package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.CreateOrderRequest;
import dev.payment.paymentservice.dto.CreatePaymentResponse;
import dev.payment.paymentservice.entity.Outbox;
import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.repository.OutboxRepository;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.LedgerEntryRepository;
import dev.payment.paymentservice.repository.WebhookInboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private WebhookInboxEventRepository webhookInboxEventRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private PaymentService paymentService;
    private ObjectMapper objectMapper;

    private Payment payment;
    private String orderId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        paymentService = new PaymentService(
                paymentRepository,
                outboxRepository,
                ledgerEntryRepository,
                webhookInboxEventRepository,
                redisTemplate,
                objectMapper
        );

        orderId = "order_test_" + UUID.randomUUID().toString().substring(0, 8);
        payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .amount(BigDecimal.valueOf(1000))
                .currency("USD")
                .status(PaymentStatus.CREATED)
                .merchantId("merchant_123")
                .build();
    }

    @Test
    void createPayment_ValidRequest_ShouldCreatePayment() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderId(orderId);
        request.setAmount(BigDecimal.valueOf(1000));
        request.setCurrency("USD");
        request.setPaymentMethod("CARD");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
        when(outboxRepository.save(any(Outbox.class))).thenReturn(new Outbox());

        CreatePaymentResponse response = paymentService.createPayment(null, request, "merchant_123");

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(BigDecimal.valueOf(1000), response.getAmount());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void getPaymentStatus_ExistingPayment_ShouldReturnStatus() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        var result = paymentService.getPaymentStatus(orderId);

        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("CREATED", result.getStatus());
    }

    @Test
    void updatePaymentStatus_ValidTransition_ShouldUpdateStatus() {
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(outboxRepository.save(any(Outbox.class))).thenReturn(new Outbox());

        paymentService.updatePaymentStatus(payment.getId().toString(), PaymentStatus.AUTHORIZATION_PENDING);

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void verifyOtp_ValidOtp_ShouldVerifyPayment() {
        payment.setStatus(PaymentStatus.CHALLENGE_REQUIRED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.verifyOtp(payment.getId().toString(), "123456");

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void verifyOtp_InvalidOtp_ShouldFailPayment() {
        payment.setStatus(PaymentStatus.CHALLENGE_REQUIRED);
        when(paymentRepository.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        assertThrows(Exception.class, () -> paymentService.verifyOtp(payment.getId().toString(), "000000"));
    }
}
