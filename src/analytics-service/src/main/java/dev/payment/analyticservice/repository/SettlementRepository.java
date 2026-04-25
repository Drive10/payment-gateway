package dev.payment.analyticservice.repository;

import dev.payment.analyticservice.entity.SettlementMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<SettlementMetric, UUID> {
    Optional<SettlementMetric> findByMerchantId(String merchantId);
}
