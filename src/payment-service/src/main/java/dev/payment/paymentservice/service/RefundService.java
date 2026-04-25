package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.entity.Refund;
import dev.payment.paymentservice.entity.Refund.RefundStatus;
import dev.payment.paymentservice.exception.ErrorCodes;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
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
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + request.getPaymentId()));

        if (!payment.getMerchantId().equals(merchantId)) {
            throw PaymentException.badRequest("Payment does not belong to this merchant");
        }

        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            throw PaymentException.badRequest("Only captured payments can be refunded");
        }

        BigDecimal refundAmount = request.getAmount();
        BigDecimal capturedAmount = payment.getAmount();
        BigDecimal alreadyRefunded = payment.getRefundAmount() != null ? payment.getRefundAmount() : BigDecimal.ZERO;
        BigDecimal availableRefund = capturedAmount.subtract(alreadyRefunded);

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) == 0) {
            refundAmount = availableRefund;
        }

        if (refundAmount.compareTo(availableRefund) > 0) {
            throw PaymentException.badRequest("Refund amount exceeds available refund amount: " + availableRefund);
        }

        Refund refund = Refund.builder()
            .paymentId(request.getPaymentId())
            .amount(refundAmount)
            .refundedAmount(alreadyRefunded.add(refundAmount))
            .currency(payment.getCurrency())
            .status(RefundStatus.COMPLETED)
            .reason(request.getReason())
            .build();

        refund = refundRepository.save(refund);

        BigDecimal newRefundedAmount = alreadyRefunded.add(refundAmount);
        payment.setRefundAmount(newRefundedAmount);
        
        if (newRefundedAmount.compareTo(capturedAmount) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        }
        
        paymentRepository.save(payment);

        log.info("Refund created: {} for payment: {}, amount: {}", 
            refund.getRefundId(), request.getPaymentId(), refundAmount);

        return RefundResponse.builder()
            .refundId(refund.getRefundId())
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(refundAmount)
            .refundedAmount(newRefundedAmount)
            .currency(payment.getCurrency())
            .status(refund.getStatus().name())
            .reason(refund.getReason())
            .createdAt(refund.getCreatedAt())
            .build();
    }

    public RefundResponse getRefund(String refundId) {
        Refund refund = refundRepository.findByRefundId(refundId)
            .orElseThrow(() -> PaymentException.notFound("Refund not found: " + refundId));

        return toResponse(refund);
    }

    private RefundResponse toResponse(Refund refund) {
        return RefundResponse.builder()
            .refundId(refund.getRefundId())
            .paymentId(refund.getPaymentId())
            .amount(refund.getAmount())
            .refundedAmount(refund.getRefundedAmount())
            .currency(refund.getCurrency())
            .status(refund.getStatus().name())
            .reason(refund.getReason())
            .createdAt(refund.getCreatedAt())
            .build();
    }
}