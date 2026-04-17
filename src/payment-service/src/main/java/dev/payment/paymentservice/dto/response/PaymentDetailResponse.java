package dev.payment.paymentservice.dto.response;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentRefund;
import dev.payment.paymentservice.domain.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentDetailResponse(
        UUID id,
        UUID orderId,
        String orderReference,
        UUID merchantId,
        BigDecimal amount,
        String currency,
        String status,
        String provider,
        String method,
        String transactionMode,
        
        // Fee breakdown
        BigDecimal platformFee,
        BigDecimal gatewayFee,
        BigDecimal totalFee,
        BigDecimal netAmount,
        
        // Provider info
        String providerOrderId,
        String providerPaymentId,
        String providerSignature,
        boolean simulated,
        
        // Timestamps
        Instant createdAt,
        Instant updatedAt,
        Instant capturedAt,
        
        // Refund info
        BigDecimal refundedAmount,
        Integer refundCount,
        List<RefundDetail> refunds,
        
        // Transaction timeline
        List<TransactionEvent> timeline,
        
        // Notes
        String notes
) {
    public static PaymentDetailResponse from(Payment payment, List<PaymentRefund> refunds, List<PaymentTransaction> transactions) {
        List<RefundDetail> refundDetails = refunds.stream()
                .map(r -> new RefundDetail(
                        r.getId(),
                        r.getRefundReference(),
                        r.getAmount(),
                        r.getStatus().name(),
                        r.getReason(),
                        r.getCreatedAt()
                ))
                .toList();

        List<TransactionEvent> timeline = transactions.stream()
                .map(t -> new TransactionEvent(
                        t.getId(),
                        t.getType().name(),
                        t.getStatus().name(),
                        t.getAmount(),
                        t.getRemarks(),
                        t.getCreatedAt()
                ))
                .toList();

        BigDecimal totalRefund = refunds.stream()
                .map(PaymentRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal platformFee = payment.getPlatformFee() != null ? payment.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal gatewayFee = payment.getGatewayFee() != null ? payment.getGatewayFee() : BigDecimal.ZERO;
        BigDecimal totalFee = platformFee.add(gatewayFee);
        BigDecimal netAmount = payment.getAmount().subtract(totalFee);

        return new PaymentDetailResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderReference(),
                payment.getMerchantId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getProvider(),
                payment.getMethod().name(),
                payment.getTransactionMode().name(),
                
                platformFee,
                gatewayFee,
                totalFee,
                netAmount,
                
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getProviderSignature(),
                payment.isSimulated(),
                
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getStatus() == dev.payment.paymentservice.domain.enums.PaymentStatus.CAPTURED 
                        ? payment.getUpdatedAt() : null,
                
                totalRefund,
                refundDetails.size(),
                refundDetails,
                timeline,
                
                payment.getNotes()
        );
    }

    public record RefundDetail(
            UUID id,
            String refundReference,
            BigDecimal amount,
            String status,
            String reason,
            Instant createdAt
    ) {}

    public record TransactionEvent(
            UUID id,
            String type,
            String status,
            BigDecimal amount,
            String remarks,
            Instant createdAt
    ) {}
}
