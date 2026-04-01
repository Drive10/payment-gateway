package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findByIdAndOrderUser(UUID id, User user);

    Page<Payment> findByOrderUser(User user, Pageable pageable);

    Page<Payment> findByOrderUserAndStatus(User user, PaymentStatus status, Pageable pageable);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    List<Payment> findByStatusInAndUpdatedAtAfterOrderByUpdatedAtAsc(
            Collection<PaymentStatus> statuses,
            Instant updatedAt,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = 'CAPTURED'")
    BigDecimal sumCapturedAmountByMerchantId(@Param("merchantId") UUID merchantId);

    @Query("SELECT COALESCE(SUM(p.platformFee), 0) FROM Payment p WHERE p.merchantId = :merchantId")
    BigDecimal sumPlatformFeeByMerchantId(@Param("merchantId") UUID merchantId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.merchantId = :merchantId")
    long countByMerchantId(@Param("merchantId") UUID merchantId);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = :status")
    long countByMerchantIdAndStatus(@Param("merchantId") UUID merchantId, @Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.merchantId = :merchantId AND p.status = 'CAPTURED' AND p.createdAt >= :start AND p.createdAt < :end")
    BigDecimal sumCapturedAmountByMerchantIdAndDateRange(@Param("merchantId") UUID merchantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.merchantId = :merchantId AND p.createdAt >= :start AND p.createdAt < :end")
    long countByMerchantIdAndDateRange(@Param("merchantId") UUID merchantId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Page<Payment> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Payment> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status, Pageable pageable);
}
