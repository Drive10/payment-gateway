package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.RefundRequest;
import dev.payment.paymentservice.dto.RefundResponse;
import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Refund;
import dev.payment.paymentservice.entity.Refund.RefundStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public RefundResponse createRefund(RefundRequest request, String merchantId) {
        Payment payment = paymentRepository.findById(UUID.fromString(request.getPaymentId()))
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        java.math.BigDecimal refundAmount = request.getAmount();
        if (refundAmount == null) {
            refundAmount = payment.getAmount();
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

        return RefundResponse.builder()
                .refundId(refund.getId().toString())
                .paymentId(refund.getPaymentId())
                .orderId(payment.getOrderId())
                .amount(refund.getAmount())
                .refundedAmount(refund.getRefundedAmount())
                .currency(refund.getCurrency())
                .status(refund.getStatus().name())
                .reason(refund.getReason())
                .build();
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

    public Optional<Refund> getRefundsByPaymentId(String paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }
}