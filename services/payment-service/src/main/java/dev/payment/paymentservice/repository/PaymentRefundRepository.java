package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {
    Optional<PaymentRefund> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentRefund> findByProviderRefundId(String providerRefundId);
    List<PaymentRefund> findByPaymentIdOrderByCreatedAtAsc(UUID paymentId);

    @Query("select coalesce(sum(r.amount), 0) from PaymentRefund r where r.payment.id = :paymentId")
    BigDecimal sumRefundedAmountByPaymentId(UUID paymentId);
}
