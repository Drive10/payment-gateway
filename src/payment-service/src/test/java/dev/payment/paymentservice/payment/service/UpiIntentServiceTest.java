package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpiIntentService")
class UpiIntentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStateMachine paymentStateMachine;

    private UpiIntentService upiIntentService;

    @BeforeEach
    void setUp() {
        upiIntentService = new UpiIntentService(paymentRepository, paymentStateMachine);
        ReflectionTestUtils.setField(upiIntentService, "defaultVpa", "payflow@upi");
        ReflectionTestUtils.setField(upiIntentService, "merchantName", "Test Merchant");
    }

    private Payment createUpiPayment() {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("INR");
        payment.setMethod(PaymentMethod.UPI);
        payment.setTransactionMode(TransactionMode.TEST);
        payment.setStatus(PaymentStatus.CREATED);
        payment.setProvider("RAZORPAY_SIMULATOR");
        payment.setMerchantId(UUID.randomUUID());
        payment.setIdempotencyKey("test-idem-" + System.nanoTime());
        payment.setCheckoutUrl("https://checkout.test/" + payment.getId());
        payment.setProviderOrderId("order-123");
        payment.setUpdatedAt(Instant.now());
        return payment;
    }

    @Nested
    @DisplayName("createUpiIntent")
    class CreateUpiIntent {

        @Test
        @DisplayName("Should create valid UPI deep link")
        void createsValidUpiLink() {
            Payment payment = createUpiPayment();
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            UpiIntentService.UpiIntentResponse response = upiIntentService.createUpiIntent(payment, null);

            assertThat(response.upiLink()).startsWith("upi://pay?");
            assertThat(response.upiLink()).contains("pa=");
            assertThat(response.upiLink()).contains("pn=");
            assertThat(response.upiLink()).contains("am=");
            assertThat(response.transactionId()).isEqualTo(payment.getId().toString());
            assertThat(response.status()).isEqualTo("AWAITING_UPI_PAYMENT");
            assertThat(response.expiryMinutes()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should use custom VPA when provided")
        void usesCustomVpa() {
            Payment payment = createUpiPayment();
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            UpiIntentService.UpiIntentResponse response = upiIntentService.createUpiIntent(payment, "custom@upi");

            assertThat(response.upiLink()).contains("pa=custom%40upi");
            assertThat(payment.getUpiId()).isEqualTo("custom@upi");
        }

        @Test
        @DisplayName("Should throw exception for non-UPI payment")
        void rejectsNonUpiPayment() {
            Payment payment = createUpiPayment();
            payment.setMethod(PaymentMethod.CARD);

            assertThatThrownBy(() -> upiIntentService.createUpiIntent(payment, null))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("must be UPI");
        }

        @Test
        @DisplayName("Should transition payment to AWAITING_UPI_PAYMENT")
        void transitionsStateCorrectly() {
            Payment payment = createUpiPayment();
            when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

            upiIntentService.createUpiIntent(payment, null);

            verify(paymentStateMachine).transition(payment, PaymentStatus.AWAITING_UPI_PAYMENT);
        }
    }

    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("Should return true for expired UPI payment")
        void returnsTrueForExpired() {
            Payment payment = createUpiPayment();
            payment.setStatus(PaymentStatus.AWAITING_UPI_PAYMENT);
            payment.setUpdatedAt(Instant.now().minusSeconds(400)); // 6+ minutes ago

            assertThat(upiIntentService.isExpired(payment)).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-expired UPI payment")
        void returnsFalseForFresh() {
            Payment payment = createUpiPayment();
            payment.setStatus(PaymentStatus.AWAITING_UPI_PAYMENT);
            payment.setUpdatedAt(Instant.now().minusSeconds(60)); // 1 minute ago

            assertThat(upiIntentService.isExpired(payment)).isFalse();
        }

        @Test
        @DisplayName("Should return false for non-UPI payment")
        void returnsFalseForNonUpi() {
            Payment payment = createUpiPayment();
            payment.setMethod(PaymentMethod.CARD);
            payment.setUpdatedAt(Instant.now().minusSeconds(400));

            assertThat(upiIntentService.isExpired(payment)).isFalse();
        }
    }

    @Nested
    @DisplayName("findAndExpireStaleUpiPayments")
    class FindAndExpireStale {

        @Test
        @DisplayName("Should find and expire stale UPI payments")
        void expiresStalePayments() {
            Payment stalePayment = createUpiPayment();
            stalePayment.setStatus(PaymentStatus.AWAITING_UPI_PAYMENT);
            stalePayment.setUpdatedAt(Instant.now().minusSeconds(400));
            when(paymentRepository.findAll()).thenReturn(java.util.List.of(stalePayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(stalePayment);

            Optional<Payment> result = upiIntentService.findAndExpireStaleUpiPayments();

            assertThat(result).isPresent();
            verify(paymentStateMachine).transition(stalePayment, PaymentStatus.EXPIRED);
        }
    }
}
