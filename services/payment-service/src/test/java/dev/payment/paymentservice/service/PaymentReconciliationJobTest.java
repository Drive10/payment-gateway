package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentReconciliationJobTest {

    @Test
    void shouldRecordMismatchWhenProviderStateDoesNotMatchPaymentState() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentReconciliationClient reconciliationClient = mock(PaymentReconciliationClient.class);
        AuditService auditService = mock(AuditService.class);
        Payment payment = payment(PaymentStatus.CAPTURED);

        when(paymentRepository.findByStatusInAndUpdatedAtAfterOrderByUpdatedAtAsc(any(), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(payment));
        when(reconciliationClient.lookup(payment))
                .thenReturn(Optional.of(new ProviderPaymentSnapshot(
                        payment.getProviderOrderId(),
                        "provider_payment_1",
                        "CREATED",
                        payment.getAmount(),
                        payment.getCurrency(),
                        true
                )));

        PaymentReconciliationJob job = new PaymentReconciliationJob(
                paymentRepository,
                reconciliationClient,
                auditService,
                true,
                Duration.ofHours(6),
                25
        );

        job.reconcileRecentPayments();

        ArgumentCaptor<String> summaryCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).record(
                eq("PAYMENT_RECONCILIATION_MISMATCH"),
                eq("system"),
                eq("PAYMENT"),
                eq(payment.getId().toString()),
                summaryCaptor.capture()
        );
        assertThat(summaryCaptor.getValue()).contains("expectedStatus=CAPTURED").contains("actualStatus=CREATED");
    }

    @Test
    void shouldSkipAuditWhenProviderStateMatches() {
        PaymentRepository paymentRepository = mock(PaymentRepository.class);
        PaymentReconciliationClient reconciliationClient = mock(PaymentReconciliationClient.class);
        AuditService auditService = mock(AuditService.class);
        Payment payment = payment(PaymentStatus.CAPTURED);

        when(paymentRepository.findByStatusInAndUpdatedAtAfterOrderByUpdatedAtAsc(any(), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(payment));
        when(reconciliationClient.lookup(payment))
                .thenReturn(Optional.of(new ProviderPaymentSnapshot(
                        payment.getProviderOrderId(),
                        "provider_payment_1",
                        "CAPTURED",
                        payment.getAmount(),
                        payment.getCurrency(),
                        true
                )));

        PaymentReconciliationJob job = new PaymentReconciliationJob(
                paymentRepository,
                reconciliationClient,
                auditService,
                true,
                Duration.ofHours(6),
                25
        );

        job.reconcileRecentPayments();

        verify(auditService, never()).record(eq("PAYMENT_RECONCILIATION_MISMATCH"), any(), any(), any(), any());
        verify(auditService, never()).record(eq("PAYMENT_RECONCILIATION_FAILED"), any(), any(), any(), any());
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setProvider("RAZORPAY_SIMULATOR");
        payment.setProviderOrderId("order_" + UUID.randomUUID());
        payment.setAmount(new BigDecimal("2499.00"));
        payment.setCurrency("INR");
        payment.setSimulated(true);
        payment.setStatus(status);
        return payment;
    }
}
