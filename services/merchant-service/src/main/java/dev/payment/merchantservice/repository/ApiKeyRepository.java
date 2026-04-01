package dev.payment.merchantservice.repository;

import dev.payment.merchantservice.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    
    List<ApiKey> findByMerchantId(UUID merchantId);
    
    List<ApiKey> findByMerchantIdAndStatus(UUID merchantId, String status);
    
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    boolean existsByMerchantId(UUID merchantId);
}
