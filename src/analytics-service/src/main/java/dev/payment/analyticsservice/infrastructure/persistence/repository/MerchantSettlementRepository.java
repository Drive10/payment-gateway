package dev.payment.analyticsservice.repository;

import dev.payment.analyticsservice.entity.MerchantSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, UUID> {
    
    Optional<MerchantSettlement> findByMerchantId(UUID merchantId);
    
    boolean existsByMerchantId(UUID merchantId);

    List<MerchantSettlement> findByAutoSettleTrueAndCurrentBalanceGreaterThanEqual(BigDecimal minimumAmount);
}
