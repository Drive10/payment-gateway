package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@Profile("!test")
public class PaymentReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);
    private static final EnumSet<PaymentStatus> RECONCILABLE_STATUSES =
            EnumSet.of(PaymentStatus.CREATED, PaymentStatus.CAPTURED, PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED);

    private final PaymentRepository paymentRepository;
    private final PaymentReconciliationClient paymentReconciliationClient;
    private final AuditService auditService;
    private final boolean enabled;
    private final Duration lookbackWindow;
    private final int batchSize;

    public PaymentReconciliationJob(
            PaymentRepository paymentRepository,
            PaymentReconciliationClient paymentReconciliationClient,
            AuditService auditService,
            @Value("${application.reconciliation.enabled:true}") boolean enabled,
            @Value("${application.reconciliation.lookback:PT6H}") Duration lookbackWindow,
            @Value("${application.reconciliation.batch-size:50}") int batchSize
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentReconciliationClient = paymentReconciliationClient;
        this.auditService = auditService;
        this.enabled = enabled;
        this.lookbackWindow = lookbackWindow;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${application.reconciliation.fixed-delay:PT5M}",
            initialDelayString = "${application.reconciliation.initial-delay:PT1M}"
    )
    public void reconcileRecentPayments() {
        if (!enabled) {
            return;
        }

        Instant updatedAfter = Instant.now().minus(lookbackWindow);
        List<Payment> candidates = paymentRepository.findByStatusInAndUpdatedAtAfterOrderByUpdatedAtAsc(
                RECONCILABLE_STATUSES,
                updatedAfter,
                PageRequest.of(0, batchSize)
        );

        for (Payment payment : candidates) {
            reconcile(payment);
        }
    }

    private void reconcile(Payment payment) {
        try {
            Optional<ProviderPaymentSnapshot> snapshot = paymentReconciliationClient.lookup(payment);
            if (snapshot.isEmpty()) {
                return;
            }

            ProviderPaymentSnapshot providerPayment = snapshot.get();
            String expectedProviderStatus = expectedProviderStatus(payment);
            if (!expectedProviderStatus.equalsIgnoreCase(providerPayment.status())
                    || payment.getAmount().compareTo(providerPayment.amount()) != 0
                    || !payment.getCurrency().equalsIgnoreCase(providerPayment.currency())) {
                String summary = "Payment reconciliation mismatch expectedStatus=%s actualStatus=%s paymentAmount=%s providerAmount=%s paymentCurrency=%s providerCurrency=%s"
                        .formatted(
                                expectedProviderStatus,
                                providerPayment.status(),
                                payment.getAmount(),
                                providerPayment.amount(),
                                payment.getCurrency(),
                                providerPayment.currency()
                        );
                auditService.record(
                        "PAYMENT_RECONCILIATION_MISMATCH",
                        "system",
                        "PAYMENT",
                        payment.getId().toString(),
                        summary
                );
                log.warn("event=payment_reconciliation_mismatch paymentId={} providerOrderId={} expectedStatus={} actualStatus={} paymentAmount={} providerAmount={} paymentCurrency={} providerCurrency={}",
                        payment.getId(),
                        payment.getProviderOrderId(),
                        expectedProviderStatus,
                        providerPayment.status(),
                        payment.getAmount(),
                        providerPayment.amount(),
                        payment.getCurrency(),
                        providerPayment.currency());
            }
        } catch (PaymentReconciliationClient.ProviderPaymentMissingException exception) {
            auditService.record(
                    "PAYMENT_RECONCILIATION_MISSING",
                    "system",
                    "PAYMENT",
                    payment.getId().toString(),
                    "Provider record missing for providerOrderId=" + payment.getProviderOrderId()
            );
            log.warn("event=payment_reconciliation_missing paymentId={} providerOrderId={}",
                    payment.getId(), payment.getProviderOrderId());
        } catch (RuntimeException exception) {
            auditService.record(
                    "PAYMENT_RECONCILIATION_FAILED",
                    "system",
                    "PAYMENT",
                    payment.getId().toString(),
                    "Unable to reconcile payment with provider: " + exception.getClass().getSimpleName()
            );
            log.error("event=payment_reconciliation_failed paymentId={} providerOrderId={} message={}",
                    payment.getId(), payment.getProviderOrderId(), exception.getMessage(), exception);
        }
    }

    private String expectedProviderStatus(Payment payment) {
        return switch (payment.getStatus()) {
            case PENDING, CREATED, AWAITING_UPI_PAYMENT, AUTHORIZATION_PENDING, PROCESSING, AUTHORIZED -> "CREATED";
            case CAPTURED, PARTIALLY_REFUNDED, REFUNDED -> "CAPTURED";
            case EXPIRED -> "EXPIRED";
            case FAILED -> "FAILED";
        };
    }
}
