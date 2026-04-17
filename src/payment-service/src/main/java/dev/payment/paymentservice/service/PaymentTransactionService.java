package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentTransaction;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.domain.enums.TransactionStatus;
import dev.payment.paymentservice.domain.enums.TransactionType;
import dev.payment.paymentservice.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentTransactionService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionService.class);

    private final PaymentTransactionRepository transactionRepository;

    public PaymentTransactionService(PaymentTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<PaymentTransaction> findByPaymentId(UUID paymentId) {
        return transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    @Transactional
    public PaymentTransaction createTransaction(
            Payment payment,
            TransactionType type,
            TransactionStatus status,
            String remarks,
            String providerReference,
            BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setProviderReference(providerReference);
        transaction.setRemarks(remarks);
        
        PaymentTransaction saved = transactionRepository.save(transaction);
        log.info("transaction=created paymentId={} type={} status={} amount={}",
                payment.getId(), type, status, amount);
        return saved;
    }

    @Transactional
    public PaymentTransaction createPaymentInitiated(Payment payment) {
        String remarks = payment.getTransactionMode() == TransactionMode.TEST
                ? "Test simulator order created"
                : "Production payment order created";
        return createTransaction(
                payment,
                TransactionType.PAYMENT_INITIATED,
                TransactionStatus.PENDING,
                remarks,
                payment.getProviderOrderId(),
                payment.getAmount()
        );
    }

    @Transactional
    public PaymentTransaction createCaptureSuccess(Payment payment, String providerReference) {
        String remarks = payment.getTransactionMode() == TransactionMode.TEST
                ? "Test payment captured"
                : "Production payment captured";
        return createTransaction(
                payment,
                TransactionType.PAYMENT_CAPTURED,
                TransactionStatus.SUCCESS,
                remarks,
                providerReference,
                payment.getAmount()
        );
    }

    @Transactional
    public PaymentTransaction createCaptureFailed(Payment payment) {
        return createTransaction(
                payment,
                TransactionType.PAYMENT_CAPTURED,
                TransactionStatus.FAILED,
                "Payment capture failed",
                payment.getProviderOrderId(),
                payment.getAmount()
        );
    }

    @Transactional
    public PaymentTransaction createRefundRequested(Payment payment, String reason, String refundReference, BigDecimal amount) {
        String remarks = (reason == null || reason.isBlank()) ? "Refund requested" : reason;
        return createTransaction(
                payment,
                TransactionType.REFUND_REQUESTED,
                TransactionStatus.PENDING,
                remarks,
                refundReference,
                amount
        );
    }

    @Transactional
    public PaymentTransaction createRefundCompleted(Payment payment, String reason, String refundReference, BigDecimal amount) {
        String remarks = (reason == null || reason.isBlank()) ? "Refund completed" : reason;
        return createTransaction(
                payment,
                TransactionType.REFUND_COMPLETED,
                TransactionStatus.SUCCESS,
                remarks,
                refundReference,
                amount
        );
    }
}
