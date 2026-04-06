package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentReconciliationClient {

    private final List<PaymentReconciliationProvider> providers;

    public PaymentReconciliationClient(List<PaymentReconciliationProvider> providers) {
        this.providers = providers;
    }

    public Optional<ProviderPaymentSnapshot> lookup(Payment payment) {
        for (PaymentReconciliationProvider provider : providers) {
            if (provider.supports(payment)) {
                return provider.lookup(payment);
            }
        }
        return Optional.empty();
    }

    public static class ProviderPaymentMissingException extends RuntimeException {
        public ProviderPaymentMissingException(String providerOrderId) {
            super("Provider payment not found for order " + providerOrderId);
        }
    }
}
