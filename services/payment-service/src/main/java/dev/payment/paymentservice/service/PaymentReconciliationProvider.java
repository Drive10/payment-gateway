package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;

import java.util.Optional;

public interface PaymentReconciliationProvider {
    boolean supports(Payment payment);

    Optional<ProviderPaymentSnapshot> lookup(Payment payment);
}
