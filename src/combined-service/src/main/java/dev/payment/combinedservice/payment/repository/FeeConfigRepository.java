package dev.payment.combinedservice.payment.repository;

import dev.payment.combinedservice.payment.domain.FeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeConfigRepository extends JpaRepository<FeeConfig, UUID> {
    Optional<FeeConfig> findByMerchantIdAndActiveTrue(UUID merchantId);
    Optional<FeeConfig> findByMerchantId(UUID merchantId);
}
