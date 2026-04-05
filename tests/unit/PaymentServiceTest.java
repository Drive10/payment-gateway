package dev.payment.tests.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Assertions;
import java.util.*;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for Payment Service
 * Tests business logic in isolation with mocked dependencies
 */
@DisplayName("Payment Service Unit Tests")
@Tag("unit")
public class PaymentServiceTest {

    private PaymentService paymentService;
    private PaymentRepository mockRepository;
    private IdempotencyService mockIdempotencyService;

    @BeforeEach
    void setUp() {
        mockRepository = mock(PaymentRepository.class);
        mockIdempotencyService = mock(IdempotencyService.class);
        paymentService = new PaymentService(mockRepository, mockIdempotencyService);
    }

    // ============ IDEMPOTENCY TESTS ============

    @Test
    @DisplayName("Should reject duplicate payment requests with same idempotency key")
    void shouldRejectDuplicatePayments() {
        // Given
        String idempotencyKey = "unique-key-123";
        PaymentRequest request = new PaymentRequest("order-1", BigDecimal.valueOf(100), "CARD");
        
        when(mockIdempotencyService.hasBeenProcessed(idempotencyKey)).thenReturn(true);
        when(mockIdempotencyService.getExistingResult(idempotencyKey)).thenReturn(
            Optional.of(new PaymentResult("existing-payment-id", "COMPLETED"))
        );

        // When
        PaymentResult result = paymentService.processPayment(request, idempotencyKey);

        // Then
        assertEquals("existing-payment-id", result.getPaymentId());
        assertEquals("COMPLETED", result.getStatus());
        verify(mockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should process new payment when idempotency key is new")
    void shouldProcessNewPayment() {
        // Given
        String idempotencyKey = "new-key-456";
        PaymentRequest request = new PaymentRequest("order-2", BigDecimal.valueOf(200), "UPI");
        
        when(mockIdempotencyService.hasBeenProcessed(idempotencyKey)).thenReturn(false);
        when(mockRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("new-payment-id");
            return p;
        });

        // When
        PaymentResult result = paymentService.processPayment(request, idempotencyKey);

        // Then
        assertEquals("new-payment-id", result.getPaymentId());
        assertEquals("PENDING", result.getStatus());
        verify(mockRepository).save(any(Payment.class));
        verify(mockIdempotencyService).markAsProcessed(eq(idempotencyKey), any());
    }

    // ============ PAYMENT VALIDATION TESTS ============

    @Nested
    @DisplayName("Payment Validation Tests")
    class PaymentValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {"CARD", "UPI", "NETBANKING", "WALLET"})
        @DisplayName("Should accept valid payment methods")
        void shouldAcceptValidPaymentMethods(String method) {
            PaymentRequest request = new PaymentRequest("order-1", BigDecimal.valueOf(100), method);
            assertTrue(paymentService.isValidPaymentMethod(request));
        }

        @Test
        @DisplayName("Should reject negative amounts")
        void shouldRejectNegativeAmounts() {
            PaymentRequest request = new PaymentRequest("order-1", BigDecimal.valueOf(-100), "CARD");
            assertFalse(paymentService.isValidAmount(request));
        }

        @Test
        @DisplayName("Should reject amount exceeding limit")
        void shouldRejectAmountExceedingLimit() {
            PaymentRequest request = new PaymentRequest("order-1", BigDecimal.valueOf(1_000_000), "CARD");
            assertFalse(paymentService.isValidAmount(request));
        }

        @Test
        @DisplayName("Should accept amount within limits")
        void shouldAcceptValidAmount() {
            PaymentRequest request = new PaymentRequest("order-1", BigDecimal.valueOf(50000), "CARD");
            assertTrue(paymentService.isValidAmount(request));
        }
    }

    // ============ PAYMENT PROCESSING TESTS ============

    @Nested
    @DisplayName("Payment Processing Tests")
    class PaymentProcessingTests {

        @Test
        @DisplayName("Should calculate correct processing fee")
        void shouldCalculateCorrectFee() {
            BigDecimal amount = BigDecimal.valueOf(1000);
            BigDecimal expectedFee = BigDecimal.valueOf(25); // 2.5%
            
            BigDecimal fee = paymentService.calculateFee(amount);
            
            assertEquals(0, expectedFee.compareTo(fee));
        }

        @Test
        @DisplayName("Should generate unique transaction ID")
        void shouldGenerateUniqueTransactionId() {
            Set<String> ids = new HashSet<>();
            
            for (int i = 0; i < 1000; i++) {
                ids.add(paymentService.generateTransactionId());
            }
            
            assertEquals(1000, ids.size()); // All unique
        }
    }

    // ============ EDGE CASES ============

    @Test
    @DisplayName("Should handle concurrent payment requests safely")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // Given
        String idempotencyKey = "concurrent-key";
        PaymentRequest request = new PaymentRequest("order-1", BigDecimal.valueOf(100), "CARD");
        
        when(mockIdempotencyService.hasBeenProcessed(idempotencyKey)).thenReturn(false);
        when(mockRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId("concurrent-payment-id");
            return p;
        });

        // When - Simulate concurrent requests
        List<PaymentResult> results = new ArrayList<>();
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                results.add(paymentService.processPayment(request, idempotencyKey));
            });
            threads[i].start();
        }
        
        for (Thread t : threads) {
            t.join();
        }

        // Then - Only one payment should be processed
        verify(mockRepository, times(1)).save(any());
        assertEquals(10, results.size());
    }
}

// ============ MOCK CLASSES FOR COMPILATION ============

class PaymentService {
    private final PaymentRepository repository;
    private final IdempotencyService idempotencyService;

    public PaymentService(PaymentRepository repository, IdempotencyService idempotencyService) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
    }

    public PaymentResult processPayment(PaymentRequest request, String idempotencyKey) {
        if (idempotencyService.hasBeenProcessed(idempotencyKey)) {
            return idempotencyService.getExistingResult(idempotencyKey).orElse(null);
        }
        
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("PENDING");
        
        Payment saved = repository.save(payment);
        idempotencyService.markAsProcessed(idempotencyKey, new PaymentResult(saved.getId(), saved.getStatus()));
        
        return new PaymentResult(saved.getId(), saved.getStatus());
    }

    public boolean isValidPaymentMethod(PaymentRequest request) {
        return request.getPaymentMethod() != null && 
               !request.getPaymentMethod().isEmpty();
    }

    public boolean isValidAmount(PaymentRequest request) {
        return request.getAmount() != null && 
               request.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
               request.getAmount().compareTo(BigDecimal.valueOf(100000)) <= 0;
    }

    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(0.025));
    }

    public String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString();
    }
}

record PaymentRequest(String orderId, BigDecimal amount, String paymentMethod) {}
record PaymentResult(String paymentId, String status) {}
class Payment {
    private String id;
    private String orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
interface PaymentRepository {
    Payment save(Payment payment);
}
interface IdempotencyService {
    boolean hasBeenProcessed(String key);
    Optional<PaymentResult> getExistingResult(String key);
    void markAsProcessed(String key, PaymentResult result);
}
