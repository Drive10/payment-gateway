package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.PaymentRefund;
import dev.payment.paymentservice.payment.domain.PaymentTransaction;
import dev.payment.paymentservice.payment.domain.User;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.dto.response.PaymentDetailResponse;
import dev.payment.paymentservice.payment.dto.response.PaymentResponse;
import dev.payment.paymentservice.payment.dto.response.PaymentTransactionResponse;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.repository.PaymentRefundRepository;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import dev.payment.paymentservice.payment.repository.PaymentTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentQueryService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentTransactionRepository transactionRepository;

    public PaymentQueryService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository refundRepository,
            PaymentTransactionRepository transactionRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.transactionRepository = transactionRepository;
    }

    public Page<PaymentResponse> findAll(PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = status == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByStatus(status, pageable);
        return payments.map(this::toResponse);
    }

    public Page<PaymentResponse> findByMerchant(UUID merchantId, PaymentStatus status, Pageable pageable) {
        Page<Payment> payments = status == null
                ? paymentRepository.findByMerchantId(merchantId, pageable)
                : paymentRepository.findByMerchantIdAndStatus(merchantId, status, pageable);
        return payments.map(this::toResponse);
    }

    public PaymentResponse findById(UUID paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        return toResponse(payment);
    }

    public PaymentDetailResponse findDetailById(UUID paymentId) {
        Payment payment = getPaymentOrThrow(paymentId);
        List<PaymentRefund> refunds = refundRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
        List<PaymentTransaction> transactions = transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
        return PaymentDetailResponse.from(payment, refunds, transactions);
    }

    public List<PaymentRefund> findRefundsByPaymentId(UUID paymentId) {
        return refundRepository.findByPaymentIdOrderByCreatedAtAsc(paymentId);
    }

    public List<PaymentTransaction> findTransactionsByPaymentId(UUID paymentId) {
        return transactionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    private Payment getPaymentOrThrow(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
    }

    private PaymentResponse toResponse(Payment payment) {
        List<PaymentTransactionResponse> transactions = transactionRepository
                .findByPaymentIdOrderByCreatedAtDesc(payment.getId())
                .stream()
                .map(this::toTransactionResponse)
                .toList();

        String orderRef = buildOrderRef(payment.getOrderId());
        
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                orderRef,
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getMethod().name(),
                payment.getTransactionMode().name(),
                payment.getStatus().name(),
                payment.getCheckoutUrl(),
                payment.isSimulated(),
                payment.getProviderSignature(),
                payment.getNotes(),
                payment.getCreatedAt(),
                transactions
        );
    }

    private PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getProviderReference(),
                transaction.getRemarks(),
                transaction.getCreatedAt()
        );
    }

    private String buildOrderRef(UUID orderId) {
        if (orderId == null) {
            return "ORD-UNKNOWN";
        }
        String idStr = orderId.toString();
        return "ORD-" + (idStr.length() >= 8 ? idStr.substring(0, 8).toUpperCase() : idStr.toUpperCase());
    }
}
