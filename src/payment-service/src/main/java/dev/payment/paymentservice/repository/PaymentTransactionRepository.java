package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    List<PaymentTransaction> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}
