package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;

import java.util.Optional;

public interface PaymentReconciliationProvider {
    boolean supports(Payment payment);

    Optional<ProviderPaymentSnapshot> lookup(Payment payment);
}
