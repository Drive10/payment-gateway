package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
