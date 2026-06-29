package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentRepository paymentRepository;
    private OutboxRepository outboxRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private WebhookInboxEventRepository webhookInboxEventRepository;
    private StringRedisTemplate redisTemplate;
    private LedgerService ledgerService;
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        outboxRepository = mock(OutboxRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        webhookInboxEventRepository = mock(WebhookInboxEventRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ledgerService = mock(LedgerService.class);
        valueOperations = mock(ValueOperations.class);

        objectMapper = new ObjectMapper();
        paymentService = new PaymentService(paymentRepository, outboxRepository, ledgerEntryRepository,
                webhookInboxEventRepository, redisTemplate, ledgerService, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void createPayment_ShouldCreatePaymentAndSaveOutboxEvent() {
        String idempotencyKey = "idem-1";
        String merchantId = "merchant-123";
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderId("order-1");
        request.setAmount(new BigDecimal("5000"));
        request.setCurrency("INR");
        request.setPaymentMethod("CARD");

        Payment savedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId("order-1")
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .merchantId(merchantId)
                .paymentMethod("CARD")
                .correlationId(UUID.randomUUID().toString())
                .clientSecret("pi_test")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);

        assertNotNull(response);
        assertEquals(savedPayment.getId().toString(), response.getPaymentId());
        assertEquals("order-1", response.getOrderId());
        assertEquals("Payment intent created", response.getMessage());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(outboxRepository, times(1)).save(any(Outbox.class));
    }

    @Test
    void createPayment_WithIdempotencyKeyAndCachedResponse_ShouldReturnCached() {
        String idempotencyKey = "idem-existing";
        String merchantId = "merchant-123";
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderId("order-2");
        request.setAmount(new BigDecimal("1000"));
        request.setCurrency("USD");

        when(valueOperations.setIfAbsent("idempotency:" + idempotencyKey, "IN_PROGRESS", Duration.ofMinutes(30)))
                .thenReturn(false);
        when(valueOperations.get("idempotency:" + idempotencyKey)).thenReturn(
                "{\"paymentId\":\"cached-id\",\"orderId\":\"order-2\",\"amount\":1000,\"currency\":\"USD\",\"status\":\"CREATED\",\"checkoutUrl\":null,\"merchantId\":null,\"clientSecret\":null,\"requiresAction\":false,\"nextAction\":null,\"message\":null,\"transactionId\":null}");

        CreatePaymentResponse response = paymentService.createPayment(idempotencyKey, request, merchantId);

        assertNotNull(response);
        assertEquals("cached-id", response.getPaymentId());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void confirmPaymentIntent_WithCardMethod_ShouldReturnChallengeRequired() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setPaymentMethod("CARD");

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CREATED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        CreatePaymentResponse response = paymentService.confirmPaymentIntent(paymentId, request, null);

        assertNotNull(response);
        assertEquals(Boolean.TRUE, response.getRequiresAction());
        assertEquals("VERIFY_OTP", response.getNextAction());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void confirmPaymentIntent_WithNonCardMethod_ShouldReturnAuthorized() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);
        ConfirmPaymentRequest request = new ConfirmPaymentRequest();
        request.setPaymentMethod("UPI");

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CREATED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        CreatePaymentResponse response = paymentService.confirmPaymentIntent(paymentId, request, null);

        assertNotNull(response);
        assertEquals(Boolean.FALSE, response.getRequiresAction());
        assertEquals("Payment authorized", response.getMessage());
    }

    @Test
    void confirmPaymentIntent_WithNonExistentPayment_ShouldThrowException() {
        String paymentId = UUID.randomUUID().toString();
        when(paymentRepository.findById(UUID.fromString(paymentId))).thenReturn(Optional.empty());

        assertThrows(PaymentException.class,
                () -> paymentService.confirmPaymentIntent(paymentId, new ConfirmPaymentRequest(), null));
    }

    @Test
    void capturePaymentIntent_ShouldTransitionToCaptured() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.AUTHORIZED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByIdWithLock(paymentUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        CreatePaymentResponse response = paymentService.capturePaymentIntent(paymentId, null);

        assertNotNull(response);
        assertEquals(PaymentStatus.CAPTURED.name(), response.getStatus());
        assertEquals("Payment captured", response.getMessage());
        verify(ledgerService, times(1)).createPaymentJournal(payment);
    }

    @Test
    void capturePaymentIntent_FromNonAuthorizedState_ShouldThrowException() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CREATED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByIdWithLock(paymentUuid)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> paymentService.capturePaymentIntent(paymentId, null));
    }

    @Test
    void verifyOtp_WithCorrectOtp_ShouldAuthorize() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CHALLENGE_REQUIRED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByIdWithLock(paymentUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.verifyOtp(paymentId, "123456");

        verify(paymentRepository, times(1)).save(argThat(p -> p.getStatus() == PaymentStatus.AUTHORIZED));
    }

    @Test
    void verifyOtp_WithWrongOtp_ShouldFail() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CHALLENGE_REQUIRED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByIdWithLock(paymentUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        assertThrows(PaymentException.class, () -> paymentService.verifyOtp(paymentId, "wrong-otp"));
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.FAILED));
    }

    @Test
    void getPaymentStatus_ShouldReturnCorrectStatus() {
        String orderId = "order-1";
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentStatusResponse response = paymentService.getPaymentStatus(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.getOrderId());
        assertEquals(PaymentStatus.CREATED.name(), response.getStatus());
    }

    @Test
    void getPaymentStatus_WithNonExistentOrder_ShouldThrowException() {
        when(paymentRepository.findByOrderId("invalid")).thenReturn(Optional.empty());
        assertThrows(PaymentException.class, () -> paymentService.getPaymentStatus("invalid"));
    }

    @Test
    void updatePaymentStatus_WithValidTransition_ShouldSucceed() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.AUTHORIZED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.updatePaymentStatus(paymentId, PaymentStatus.CAPTURED);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.CAPTURED));
        verify(outboxRepository).save(any(Outbox.class));
    }

    @Test
    void updatePaymentStatus_WithInvalidTransition_ShouldThrowException() {
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.CREATED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findById(paymentUuid)).thenReturn(Optional.of(payment));

        assertThrows(PaymentException.class,
                () -> paymentService.updatePaymentStatus(paymentId, PaymentStatus.REFUNDED));
    }

    @Test
    void processProviderWebhook_WithPaymentAuthorized_ShouldTransitionToAuthorized() {
        String webhookId = "wh-1";
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.AUTHORIZATION_PENDING)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByIdWithLock(paymentUuid)).thenReturn(Optional.of(payment));
        when(webhookInboxEventRepository.existsByWebhookId(webhookId)).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        paymentService.processProviderWebhook(webhookId, "payment.authorized", paymentId, "{}");

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.AUTHORIZED));
    }

    @Test
    void processProviderWebhook_DuplicateWebhook_ShouldBeIdempotent() {
        String webhookId = "wh-duplicate";
        String paymentId = UUID.randomUUID().toString();
        UUID paymentUuid = UUID.fromString(paymentId);

        Payment payment = Payment.builder()
                .id(paymentUuid)
                .status(PaymentStatus.AUTHORIZED)
                .amount(new BigDecimal("5000"))
                .currency("INR")
                .merchantId("merchant-123")
                .build();

        when(paymentRepository.findByIdWithLock(paymentUuid)).thenReturn(Optional.of(payment));
        when(webhookInboxEventRepository.existsByWebhookId(webhookId)).thenReturn(true);

        paymentService.processProviderWebhook(webhookId, "payment.authorized", paymentId, "{}");

        verify(webhookInboxEventRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getMerchantOrders_ShouldReturnOrders() {
        String merchantId = "merchant-123";
        Payment payment1 = Payment.builder()
                .id(UUID.randomUUID()).orderId("order-1")
                .amount(new BigDecimal("1000")).currency("INR")
                .status(PaymentStatus.CAPTURED).merchantId(merchantId)
                .build();
        Payment payment2 = Payment.builder()
                .id(UUID.randomUUID()).orderId("order-2")
                .amount(new BigDecimal("2000")).currency("INR")
                .status(PaymentStatus.CREATED).merchantId(merchantId)
                .build();

        when(paymentRepository.findByMerchantId(merchantId)).thenReturn(List.of(payment1, payment2));

        List<PaymentStatusResponse> orders = paymentService.getMerchantOrders(merchantId);
        assertEquals(2, orders.size());
    }
}
