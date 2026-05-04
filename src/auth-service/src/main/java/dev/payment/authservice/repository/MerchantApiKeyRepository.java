package dev.payment.authservice.repository;

import dev.payment.authservice.entity.MerchantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, UUID> {

    @Query("SELECT k FROM MerchantApiKey k WHERE k.keyHash = :keyHash AND k.isActive = true AND k.revokedAt IS NULL AND (k.expiresAt IS NULL OR k.expiresAt > CURRENT_TIMESTAMP)")
    Optional<MerchantApiKey> findActiveByKeyHash(String keyHash);

    @Query("SELECT k FROM MerchantApiKey k WHERE k.merchantId = :merchantId AND k.isActive = true")
    List<MerchantApiKey> findActiveByMerchantId(UUID merchantId);

    @Query("SELECT k FROM MerchantApiKey k WHERE k.keyPrefix = :prefix AND k.isActive = true")
    List<MerchantApiKey> findActiveByPrefix(String prefix);

    @Query("SELECT k FROM MerchantApiKey k WHERE k.merchantId = :merchantId AND k.isActive = true AND k.revokedAt IS NULL")
    List<MerchantApiKey> findActiveKeysForMerchant(UUID merchantId);

    @Query("SELECT k FROM MerchantApiKey k WHERE k.id = :id")
    Optional<MerchantApiKey> findById(UUID id);
}