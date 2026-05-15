package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.RefundRequest;
import dev.payment.paymentservice.dto.RefundResponse;
import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Refund;
import dev.payment.paymentservice.entity.Refund.RefundStatus;
import dev.payment.paymentservice.entity.LedgerEntry;
import dev.payment.paymentservice.exception.PaymentException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import dev.payment.paymentservice.repository.LedgerEntryRepository;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REFUND_IDEMPOTENCY_PREFIX = "refund-idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);

    @Transactional
    public RefundResponse createRefund(RefundRequest request, String merchantId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String cached = redisTemplate.opsForValue().get(REFUND_IDEMPOTENCY_PREFIX + idempotencyKey);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, RefundResponse.class);
                } catch (JsonProcessingException ignored) {
                }
            }
        }

        Payment payment = paymentRepository.findById(UUID.fromString(request.getPaymentId()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != Payment.PaymentStatus.CAPTURED) {
            throw PaymentException.badRequest("Only captured payments can be refunded");
        }

        java.math.BigDecimal refundAmount = request.getAmount();
        java.math.BigDecimal alreadyRefunded = payment.getRefundAmount() != null ? payment.getRefundAmount() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal remainingBalance = payment.getAmount().subtract(alreadyRefunded);

        if (refundAmount == null) {
            refundAmount = remainingBalance;
        }

        if (refundAmount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw PaymentException.badRequest("Refund amount must be positive");
        }

        if (refundAmount.compareTo(remainingBalance) > 0) {
            throw PaymentException.badRequest(
                "Refund amount " + refundAmount + " exceeds remaining balance " + remainingBalance
            );
        }

        Refund refund = Refund.builder()
                .paymentId(payment.getId().toString())
                .amount(refundAmount)
                .currency(payment.getCurrency())
                .refundedAmount(refundAmount)
                .status(RefundStatus.PENDING)
                .reason(request.getReason())
                .build();

        refund = refundRepository.save(refund);

        // Update payment with refund information
        payment.setRefundAmount(payment.getRefundAmount().add(refundAmount));
        if (payment.getRefundAmount().compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
        }
        paymentRepository.save(payment);

        persistLedgerEntry(payment.getId().toString(), refund.getId().toString(), "REFUND_DEBIT_MERCHANT", refundAmount, payment.getCurrency(),
                payment.getId() + ":refund_debit:" + refund.getId(), payment.getMerchantId(), LedgerEntry.AccountType.MERCHANT_RECEIVABLE);
        persistLedgerEntry(payment.getId().toString(), refund.getId().toString(), "REFUND_CREDIT_CUSTOMER", refundAmount, payment.getCurrency(),
                payment.getId() + ":refund_credit:" + refund.getId(), payment.getMerchantId() + "_CUSTOMER", LedgerEntry.AccountType.CUSTOMER_ESCROW);

        RefundResponse response = RefundResponse.builder()
                .refundId(refund.getId().toString())
                .paymentId(refund.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(refund.getAmount())
                .refundedAmount(refund.getRefundedAmount())
                .currency(refund.getCurrency())
                .status(refund.getStatus().name())
                .reason(refund.getReason())
                .build();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            try {
                redisTemplate.opsForValue().set(REFUND_IDEMPOTENCY_PREFIX + idempotencyKey, objectMapper.writeValueAsString(response), IDEMPOTENCY_TTL);
            } catch (JsonProcessingException ignored) {
            }
        }

        return response;
    }

    @Transactional
    public RefundResponse createRefund(RefundRequest request, String merchantId) {
        return createRefund(request, merchantId, null);
    }

    public RefundResponse getRefund(String refundId) {
        Refund refund = refundRepository.findById(UUID.fromString(refundId))
                .orElseThrow(() -> new IllegalArgumentException("Refund not found"));

        return RefundResponse.builder()
                .refundId(refund.getId().toString())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .refundedAmount(refund.getRefundedAmount())
                .currency(refund.getCurrency())
                .status(refund.getStatus().name())
                .reason(refund.getReason())
                .build();
    }

    public List<Refund> getRefundsByPaymentId(String paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    private void persistLedgerEntry(String paymentId, String refundId, String entryTypeStr, java.math.BigDecimal amount, String currency, String reference,
                                     String accountId, LedgerEntry.AccountType accountType) {
        if (ledgerEntryRepository.existsByReference(reference)) {
            return;
        }

        LedgerEntry.EntryType entryType = "CUSTOMER_DEBIT".equals(entryTypeStr) || "REFUND_DEBIT_CUSTOMER".equals(entryTypeStr)
            ? LedgerEntry.EntryType.DEBIT : LedgerEntry.EntryType.CREDIT;
        
        LedgerEntry entry = LedgerEntry.builder()
                .accountId(accountId)
                .accountType(accountType)
                .entryType(entryType)
                .amount(amount)
                .currency(currency)
                .reference(reference)
                .paymentId(paymentId)
                .refundId(refundId)
                .build();
        ledgerEntryRepository.save(entry);
    }
}
