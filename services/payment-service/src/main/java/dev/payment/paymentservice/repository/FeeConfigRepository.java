package dev.payment.paymentservice.repository;

import dev.payment.paymentservice.domain.FeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeConfigRepository extends JpaRepository<FeeConfig, UUID> {
    Optional<FeeConfig> findByMerchantIdAndActiveTrue(UUID merchantId);
    Optional<FeeConfig> findByMerchantId(UUID merchantId);
}
