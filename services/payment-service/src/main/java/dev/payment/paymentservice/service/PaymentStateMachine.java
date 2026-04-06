package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;

@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, EnumSet<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.CAPTURED, PaymentStatus.FAILED),
            PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED),
            PaymentStatus.CAPTURED, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED),
            PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED)
    );

    public void transition(Payment payment, PaymentStatus targetStatus) {
        PaymentStatus current = payment.getStatus();
        if (current == targetStatus) {
            return;
        }

        EnumSet<PaymentStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(current, EnumSet.noneOf(PaymentStatus.class));
        if (!allowedTargets.contains(targetStatus)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVALID_PAYMENT_STATE_TRANSITION",
                    "Cannot transition payment from " + current + " to " + targetStatus
            );
        }

        payment.setStatus(targetStatus);
    }
}
