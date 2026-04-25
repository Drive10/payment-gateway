package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {
    Optional<Refund> findByRefundId(String refundId);
    Optional<Refund> findByPaymentId(String paymentId);
}