package dev.payment.orderservice.service;

import dev.payment.orderservice.entity.ApiKey;
import dev.payment.orderservice.entity.Merchant;
import dev.payment.orderservice.exception.CryptoOperationException;
import dev.payment.orderservice.repository.ApiKeyRepository;
import dev.payment.orderservice.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String KEY_PREFIX = "sk_live_";
    private static final Set<String> VALID_PERMISSIONS = Set.of(
            "payments:create",
            "payments:read",
            "refunds:create",
            "refunds:read",
            "orders:create",
            "orders:read",
            "customers:create",
            "customers:read",
            "webhooks:manage",
            "analytics:read"
    );

    private final ApiKeyRepository apiKeyRepository;
    private final MerchantRepository merchantRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, MerchantRepository merchantRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.merchantRepository = merchantRepository;
    }

    @Transactional
    public ApiKeyResult createApiKey(UUID merchantId, String name, String description, 
                                     List<String> permissions, Integer rateLimitPerMinute,
                                     Integer rateLimitPerDay, Instant expiresAt) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        if (!"VERIFIED".equals(merchant.getKycStatus())) {
            throw new IllegalStateException("Cannot create API key: KYC not verified");
        }

        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);
        String keyPrefix = rawKey.substring(0, 12);

        ApiKey apiKey = new ApiKey();
        apiKey.setMerchantId(merchantId);
        apiKey.setKeyPrefix(keyPrefix);
        apiKey.setKeyHash(keyHash);
        apiKey.setName(name);
        apiKey.setDescription(description);
        apiKey.setPermissions(permissions != null ? String.join(",", permissions) : "payments:create,payments:read");
        apiKey.setRateLimitPerMinute(rateLimitPerMinute != null ? rateLimitPerMinute : 100);
        apiKey.setRateLimitPerDay(rateLimitPerDay != null ? rateLimitPerDay : 10000);
        apiKey.setExpiresAt(expiresAt);
        apiKey.setStatus("ACTIVE");

        apiKeyRepository.save(apiKey);

        log.info("Created API key '{}' for merchant {}", name, merchantId);

        return new ApiKeyResult(rawKey, apiKey);
    }

    public List<ApiKey> getMerchantApiKeys(UUID merchantId) {
        return apiKeyRepository.findByMerchantId(merchantId);
    }

    public List<ApiKey> getActiveApiKeys(UUID merchantId) {
        return apiKeyRepository.findByMerchantIdAndStatus(merchantId, "ACTIVE");
    }

    public Optional<ApiKey> validateApiKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        String keyHash = hashKey(rawKey);
        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(key -> "ACTIVE".equals(key.getStatus()))
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public void recordKeyUsage(UUID apiKeyId, String ipAddress) {
        apiKeyRepository.findById(apiKeyId).ifPresent(key -> {
            key.setLastUsedAt(Instant.now());
            key.setLastUsedIp(ipAddress);
            apiKeyRepository.save(key);
        });
    }

    @Transactional
    public ApiKeyResult rotateApiKey(UUID apiKeyId) {
        ApiKey existingKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));

        existingKey.setStatus("ROTATED");
        existingKey.setRotatedAt(Instant.now());
        apiKeyRepository.save(existingKey);

        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);
        String keyPrefix = rawKey.substring(0, 12);

        ApiKey newKey = new ApiKey();
        newKey.setMerchantId(existingKey.getMerchantId());
        newKey.setKeyPrefix(keyPrefix);
        newKey.setKeyHash(keyHash);
        newKey.setName(existingKey.getName() + " (Rotated)");
        newKey.setDescription(existingKey.getDescription());
        newKey.setPermissions(existingKey.getPermissions());
        newKey.setRateLimitPerMinute(existingKey.getRateLimitPerMinute());
        newKey.setRateLimitPerDay(existingKey.getRateLimitPerDay());
        newKey.setExpiresAt(existingKey.getExpiresAt());
        newKey.setStatus("ACTIVE");

        apiKeyRepository.save(newKey);

        log.info("Rotated API key {} for merchant {}", apiKeyId, existingKey.getMerchantId());

        return new ApiKeyResult(rawKey, newKey);
    }

    @Transactional
    public void revokeApiKey(UUID apiKeyId) {
        ApiKey key = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));

        key.setStatus("REVOKED");
        apiKeyRepository.save(key);

        log.info("Revoked API key {}", apiKeyId);
    }

    @Transactional
    public void updateApiKey(UUID apiKeyId, String name, String description, 
                             List<String> permissions, Integer rateLimitPerMinute,
                             Integer rateLimitPerDay) {
        ApiKey key = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));

        if (name != null) key.setName(name);
        if (description != null) key.setDescription(description);
        if (permissions != null) key.setPermissions(String.join(",", permissions));
        if (rateLimitPerMinute != null) key.setRateLimitPerMinute(rateLimitPerMinute);
        if (rateLimitPerDay != null) key.setRateLimitPerDay(rateLimitPerDay);

        apiKeyRepository.save(key);

        log.info("Updated API key {}", apiKeyId);
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash API key: {}", e.getMessage(), e);
            throw new CryptoOperationException("Failed to hash API key", e);
        }
    }

    public record ApiKeyResult(String rawKey, ApiKey apiKey) {
    }
}
