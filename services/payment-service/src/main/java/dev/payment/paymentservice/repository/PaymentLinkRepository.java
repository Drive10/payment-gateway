package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.PaymentLink;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
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