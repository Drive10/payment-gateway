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

    // Valid state transitions - backend is single source of truth
    // PENDING: Initial state when payment record is created
    // CREATED: Payment initiated with provider, awaiting user authorization
    // AUTHORIZATION_PENDING: Awaiting OTP/3D secure verification
    // AUTHORIZED: Payment authorized, can now be captured
    // PROCESSING: Capture in progress with provider
    // CAPTURED: Money received - final success
    // FAILED: Payment failed - final failure
    // EXPIRED: No action taken within time window
    private static final Map<PaymentStatus, EnumSet<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentStatus.PENDING, EnumSet.of(PaymentStatus.CREATED, PaymentStatus.FAILED, PaymentStatus.EXPIRED),
            PaymentStatus.CREATED, EnumSet.of(PaymentStatus.AUTHORIZATION_PENDING, PaymentStatus.AUTHORIZED, PaymentStatus.FAILED, PaymentStatus.EXPIRED),
            PaymentStatus.AUTHORIZATION_PENDING, EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.FAILED, PaymentStatus.EXPIRED),
            PaymentStatus.AUTHORIZED, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.EXPIRED),
            PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.EXPIRED),
            PaymentStatus.CAPTURED, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED),
            PaymentStatus.PARTIALLY_REFUNDED, EnumSet.of(PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED),
            PaymentStatus.FAILED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.EXPIRED, EnumSet.noneOf(PaymentStatus.class),
            PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class)
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
                    "Cannot transition payment from " + current + " to " + targetStatus + ". Allowed: " + allowedTargets
            );
        }

        PaymentStatus previousStatus = current;
        payment.setStatus(targetStatus);
        
        // Log state transition for debugging/tracing
        payment.setNotes(previousStatus + " -> " + targetStatus);
    }
}
