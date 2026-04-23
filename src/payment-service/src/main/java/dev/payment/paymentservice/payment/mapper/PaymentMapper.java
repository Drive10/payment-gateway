package dev.payment.paymentservice.payment.mapper;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.PaymentRefund;
import dev.payment.paymentservice.payment.domain.PaymentTransaction;
import dev.payment.paymentservice.payment.dto.response.PaymentDetailResponse;
import dev.payment.paymentservice.payment.dto.response.PaymentResponse;
import dev.payment.paymentservice.payment.dto.response.RefundResponse;
import dev.payment.paymentservice.payment.dto.response.PaymentTransactionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment, List<PaymentTransaction> transactions) {
        List<PaymentTransactionResponse> transactionResponses = transactions.stream()
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
                payment.getMethod() != null ? payment.getMethod().name() : null,
                payment.getTransactionMode() != null ? payment.getTransactionMode().name() : null,
                payment.getStatus() != null ? payment.getStatus().name() : null,
                payment.getCheckoutUrl(),
                payment.isSimulated(),
                payment.getProviderSignature(),
                payment.getNotes(),
                payment.getCreatedAt(),
                transactionResponses
        );
    }

    public PaymentDetailResponse toDetailResponse(
            Payment payment,
            List<PaymentRefund> refunds,
            List<PaymentTransaction> transactions) {
        List<RefundResponse> refundResponses = refunds.stream()
                .map(this::toRefundResponse)
                .toList();

        List<PaymentTransactionResponse> transactionResponses = transactions.stream()
                .map(this::toTransactionResponse)
                .toList();

        return PaymentDetailResponse.from(payment, refunds, transactions);
    }

    public RefundResponse toRefundResponse(PaymentRefund refund) {
        Payment payment = refund.getPayment();
        String orderRef = buildOrderRef(payment.getOrderId());

        return new RefundResponse(
                refund.getId(),
                refund.getRefundReference(),
                payment.getId(),
                orderRef,
                refund.getAmount(),
                payment.getRefundedAmount(),
                payment.getStatus() != null ? payment.getStatus().name() : null,
                refund.getReason(),
                refund.getCreatedAt()
        );
    }

    public PaymentTransactionResponse toTransactionResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(
                transaction.getId(),
                transaction.getType() != null ? transaction.getType().name() : null,
                transaction.getStatus() != null ? transaction.getStatus().name() : null,
                transaction.getAmount(),
                transaction.getProviderReference(),
                transaction.getRemarks(),
                transaction.getCreatedAt()
        );
    }

    private String buildOrderRef(java.util.UUID orderId) {
        if (orderId == null) {
            return "ORD-UNKNOWN";
        }
        String idStr = orderId.toString();
        return "ORD-" + (idStr.length() >= 8 ? idStr.substring(0, 8).toUpperCase() : idStr.toUpperCase());
    }
}
