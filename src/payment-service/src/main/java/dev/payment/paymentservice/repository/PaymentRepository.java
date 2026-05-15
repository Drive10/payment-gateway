package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(UUID id);
}
