package dev.payment.settlementservice.repository;

import dev.payment.settlementservice.entity.MerchantSettlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantSettlementRepository extends JpaRepository<MerchantSettlement, UUID> {
    
    Optional<MerchantSettlement> findByMerchantId(UUID merchantId);
    
    boolean existsByMerchantId(UUID merchantId);
}
