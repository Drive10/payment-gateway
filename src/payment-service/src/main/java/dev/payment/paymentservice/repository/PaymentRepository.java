package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByCorrelationId(String correlationId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    boolean existsByCorrelationId(String correlationId);
    List<Payment> findByMerchantId(String merchantId);
    List<Payment> findByStatusIn(List<PaymentStatus> statuses);
}
