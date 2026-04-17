package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;
    private Payment payment;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.PENDING);
    }

    @Test
    void transition_fromPendingToCreated_shouldSucceed() {
        stateMachine.transition(payment, PaymentStatus.CREATED);

        assertEquals(PaymentStatus.CREATED, payment.getStatus());
        assertTrue(payment.getNotes().contains("PENDING -> CREATED"));
    }

    @Test
    void transition_fromPendingToFailed_shouldSucceed() {
        stateMachine.transition(payment, PaymentStatus.FAILED);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void transition_sameState_shouldBeNoOp() {
        payment.setStatus(PaymentStatus.CREATED);
        stateMachine.transition(payment, PaymentStatus.CREATED);

        assertEquals(PaymentStatus.CREATED, payment.getStatus());
        assertNull(payment.getNotes());
    }

    @Test
    void transition_fromPendingToCaptured_shouldThrow() {
        ApiException exception = assertThrows(ApiException.class,
                () -> stateMachine.transition(payment, PaymentStatus.CAPTURED));

        assertEquals("INVALID_PAYMENT_STATE_TRANSITION", exception.getErrorCode());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void transition_fromCreatedToAwaitingUpi_shouldSucceed() {
        payment.setStatus(PaymentStatus.CREATED);
        stateMachine.transition(payment, PaymentStatus.AWAITING_UPI_PAYMENT);

        assertEquals(PaymentStatus.AWAITING_UPI_PAYMENT, payment.getStatus());
    }

    @Test
    void transition_fromAwaitingUpiToCaptured_shouldSucceed() {
        payment.setStatus(PaymentStatus.AWAITING_UPI_PAYMENT);
        stateMachine.transition(payment, PaymentStatus.CAPTURED);

        assertEquals(PaymentStatus.CAPTURED, payment.getStatus());
    }

    @Test
    void transition_fromCapturedToPartiallyRefunded_shouldSucceed() {
        payment.setStatus(PaymentStatus.CAPTURED);
        stateMachine.transition(payment, PaymentStatus.PARTIALLY_REFUNDED);

        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, payment.getStatus());
    }

    @Test
    void transition_fromPartiallyRefundedToRefunded_shouldSucceed() {
        payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        stateMachine.transition(payment, PaymentStatus.REFUNDED);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void transition_fromFailed_shouldThrow() {
        payment.setStatus(PaymentStatus.FAILED);

        assertThrows(ApiException.class,
                () -> stateMachine.transition(payment, PaymentStatus.CAPTURED));
    }

    @Test
    void transition_fromRefunded_shouldThrow() {
        payment.setStatus(PaymentStatus.REFUNDED);

        assertThrows(ApiException.class,
                () -> stateMachine.transition(payment, PaymentStatus.CAPTURED));
    }

    @Test
    void transition_fromExpired_shouldThrow() {
        payment.setStatus(PaymentStatus.EXPIRED);

        assertThrows(ApiException.class,
                () -> stateMachine.transition(payment, PaymentStatus.CAPTURED));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING, CREATED",
            "PENDING, FAILED",
            "CREATED, AUTHORIZATION_PENDING",
            "CREATED, AUTHORIZED",
            "CREATED, PROCESSING",
            "AUTHORIZATION_PENDING, AUTHORIZED",
            "AUTHORIZATION_PENDING, FAILED",
            "AUTHORIZED, PROCESSING",
            "AUTHORIZED, CAPTURED",
            "PROCESSING, CAPTURED",
            "PROCESSING, FAILED",
            "CAPTURED, PARTIALLY_REFUNDED",
            "CAPTURED, REFUNDED"
    })
    void validTransitions_shouldSucceed(PaymentStatus from, PaymentStatus to) {
        payment.setStatus(from);

        assertDoesNotThrow(() -> stateMachine.transition(payment, to));
        assertEquals(to, payment.getStatus());
    }

    @ParameterizedTest
    @CsvSource({
            "FAILED, CAPTURED",
            "FAILED, PENDING",
            "REFUNDED, CAPTURED",
            "EXPIRED, PENDING",
            "PENDING, CAPTURED",
            "CREATED, REFUNDED"
    })
    void invalidTransitions_shouldThrow(PaymentStatus from, PaymentStatus to) {
        payment.setStatus(from);

        assertThrows(ApiException.class,
                () -> stateMachine.transition(payment, to));
    }
}