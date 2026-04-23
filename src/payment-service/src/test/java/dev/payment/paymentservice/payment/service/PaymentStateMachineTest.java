package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import dev.payment.paymentservice.payment.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PaymentStateMachine")
class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }

    private Payment createPayment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(java.util.UUID.randomUUID());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("INR");
        payment.setMethod(PaymentMethod.CARD);
        payment.setTransactionMode(TransactionMode.TEST);
        payment.setStatus(status);
        payment.setProvider("RAZORPAY_SIMULATOR");
        payment.setMerchantId(java.util.UUID.randomUUID());
        payment.setIdempotencyKey("test-idem-" + System.nanoTime());
        payment.setCheckoutUrl("https://checkout.test/" + payment.getId());
        payment.setProviderOrderId("order_" + System.nanoTime());
        return payment;
    }

    @Nested
    @DisplayName("Card Payment Flow")
    class CardPaymentFlow {

        @Test
        @DisplayName("PENDING -> CREATED transition should succeed")
        void pendingToCreated() {
            Payment payment = createPayment(PaymentStatus.PENDING);
            stateMachine.transition(payment, PaymentStatus.CREATED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
        }

        @Test
        @DisplayName("CREATED -> AUTHORIZED transition should succeed")
        void createdToAuthorized() {
            Payment payment = createPayment(PaymentStatus.CREATED);
            stateMachine.transition(payment, PaymentStatus.AUTHORIZED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }

        @Test
        @DisplayName("AUTHORIZED -> PROCESSING -> CAPTURED flow should succeed")
        void fullCaptureFlow() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);

            stateMachine.transition(payment, PaymentStatus.PROCESSING);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

            stateMachine.transition(payment, PaymentStatus.CAPTURED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        }
    }

    @Nested
    @DisplayName("UPI Payment Flow")
    class UpiPaymentFlow {

        @Test
        @DisplayName("CREATED -> AWAITING_UPI_PAYMENT transition should succeed")
        void createdToAwaitingUpi() {
            Payment payment = createPayment(PaymentStatus.CREATED);
            stateMachine.transition(payment, PaymentStatus.AWAITING_UPI_PAYMENT);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AWAITING_UPI_PAYMENT);
        }

        @Test
        @DisplayName("AWAITING_UPI_PAYMENT -> CAPTURED on success")
        void awaitingToCaptured() {
            Payment payment = createPayment(PaymentStatus.AWAITING_UPI_PAYMENT);
            stateMachine.transition(payment, PaymentStatus.CAPTURED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        }

        @Test
        @DisplayName("AWAITING_UPI_PAYMENT -> EXPIRED on timeout")
        void awaitingToExpired() {
            Payment payment = createPayment(PaymentStatus.AWAITING_UPI_PAYMENT);
            stateMachine.transition(payment, PaymentStatus.EXPIRED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("Failure Transitions")
    class FailureTransitions {

        @ParameterizedTest
        @CsvSource({
                "PENDING, FAILED",
                "CREATED, FAILED",
                "AUTHORIZATION_PENDING, FAILED",
                "AUTHORIZED, FAILED",
                "PROCESSING, FAILED",
                "AWAITING_UPI_PAYMENT, FAILED"
        })
        @DisplayName("Any state can transition to FAILED")
        void anyStateToFailed(PaymentStatus from, PaymentStatus to) {
            Payment payment = createPayment(from);
            stateMachine.transition(payment, PaymentStatus.FAILED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("Invalid Transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("CAPTURED cannot transition to AUTHORIZED")
        void capturedToAuthorizedFails() {
            Payment payment = createPayment(PaymentStatus.CAPTURED);
            assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.AUTHORIZED))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("FAILED is a terminal state")
        void failedIsTerminal() {
            Payment payment = createPayment(PaymentStatus.FAILED);
            assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.CAPTURED))
                    .isInstanceOf(ApiException.class);
        }

        @Test
        @DisplayName("PENDING cannot directly transition to CAPTURED")
        void pendingToCapturedFails() {
            Payment payment = createPayment(PaymentStatus.PENDING);
            assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.CAPTURED))
                    .isInstanceOf(ApiException.class);
        }
    }

    @Nested
    @DisplayName("Same State Transitions")
    class SameStateTransitions {

        @Test
        @DisplayName("Transitioning to same state is a no-op")
        void sameStateNoOp() {
            Payment payment = createPayment(PaymentStatus.AUTHORIZED);
            stateMachine.transition(payment, PaymentStatus.AUTHORIZED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }
    }
}
