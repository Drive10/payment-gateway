package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    Optional<PaymentRefund> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentRefund> findByProviderRefundId(String providerRefundId);
    List<PaymentRefund> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);

    @Query("select coalesce(sum(r.amount), 0) from PaymentRefund r where r.payment.id = :paymentId")
    BigDecimal sumRefundedAmountByPaymentId(@Param("paymentId") UUID paymentId);

    @Query("select coalesce(sum(r.amount), 0) from PaymentRefund r where r.payment.merchantId = :merchantId")
    BigDecimal sumRefundedAmountByMerchantId(@Param("merchantId") UUID merchantId);
}
