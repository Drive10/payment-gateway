package dev.payment.authservice.service;

import dev.payment.authservice.entity.MerchantApiKey;
import dev.payment.authservice.repository.MerchantApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantApiKeyService {

    private final MerchantApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<MerchantApiKey> validateApiKey(String rawKey) {
        if (rawKey == null || rawKey.length() < 10) {
            return Optional.empty();
        }

        String keyHash = passwordEncoder.encode(rawKey);

        return apiKeyRepository.findActiveByKeyHash(keyHash)
                .filter(key -> !key.isExpired())
                .filter(key -> !key.isRevoked());
    }

    public boolean hasScope(String rawKey, String scope) {
        return validateApiKey(rawKey)
                .map(key -> key.hasScope(scope))
                .orElse(false);
    }

    public boolean isIpAllowed(String rawKey, String clientIp) {
        return validateApiKey(rawKey)
                .map(key -> {
                    if (key.getIpWhitelist() == null || key.getIpWhitelist().isEmpty()) {
                        return true;
                    }
                    return key.getIpWhitelist().containsKey(clientIp);
                })
                .orElse(false);
    }

    public Optional<MerchantApiKey> findByKey(String rawKey) {
        if (rawKey == null) {
            return Optional.empty();
        }
        return validateApiKey(rawKey);
    }

    @Transactional
    public MerchantApiKey rotateApiKey(UUID keyId, UUID requestedBy) {
        MerchantApiKey existingKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        if (existingKey.isRevoked()) {
            throw new IllegalArgumentException("API key already revoked");
        }

        existingKey.setRevokedAt(Instant.now());
        existingKey.setRevokedBy(requestedBy);
        existingKey.setRevokedReason("Rotated");
        existingKey.setIsActive(false);
        apiKeyRepository.save(existingKey);

        log.info("Revoked API key: {} for merchant: {}", existingKey.getKeyPrefix(), existingKey.getMerchantId());
        return existingKey;
    }

    @Transactional
    public MerchantApiKey revokeAPIKey(UUID keyId, UUID requestedBy, String reason) {
        MerchantApiKey key = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));

        key.setRevokedAt(Instant.now());
        key.setRevokedBy(requestedBy);
        key.setRevokedReason(reason);
        key.setIsActive(false);
        apiKeyRepository.save(key);

        log.info("Revoked API key: {} for merchant: {}", key.getKeyPrefix(), key.getMerchantId());
        return key;
    }

    public List<MerchantApiKey> getActiveKeysForMerchant(UUID merchantId) {
        return apiKeyRepository.findActiveByMerchantId(merchantId);
    }

    public Optional<MerchantApiKey> getKeyById(UUID keyId) {
        return apiKeyRepository.findById(keyId);
    }
}