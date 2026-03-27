package dev.payment.paymentservice;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.service.PaymentStateMachine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    void shouldRejectInvalidTransition() {
        Payment payment = new Payment();
        payment.setStatus(PaymentStatus.CREATED);

        assertThatThrownBy(() -> stateMachine.transition(payment, PaymentStatus.REFUNDED))
                .isInstanceOf(ApiException.class);
    }
}
