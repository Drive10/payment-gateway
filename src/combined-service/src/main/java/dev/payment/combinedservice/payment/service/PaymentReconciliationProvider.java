package dev.payment.combinedservice.payment.service;

import dev.payment.combinedservice.payment.domain.Payment;

import java.util.Optional;

public interface PaymentReconciliationProvider {
    boolean supports(Payment payment);

    Optional<ProviderPaymentSnapshot> lookup(Payment payment);
}
