package dev.payment.paymentservice.payment.repository;

import dev.payment.paymentservice.payment.domain.PaymentLink;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentLinkRepository extends JpaRepository<PaymentLink, UUID> {
    Optional<PaymentLink> findByReferenceId(String referenceId);
    List<PaymentLink> findByMerchantId(UUID merchantId);
    List<PaymentLink> findByMerchantIdAndStatus(UUID merchantId, PaymentStatus status);
}