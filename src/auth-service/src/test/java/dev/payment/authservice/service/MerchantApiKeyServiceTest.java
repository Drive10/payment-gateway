package dev.payment.authservice.service;

import dev.payment.authservice.entity.MerchantApiKey;
import dev.payment.authservice.repository.MerchantApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MerchantApiKeyServiceTest {

    private MerchantApiKeyRepository apiKeyRepository;
    private PasswordEncoder passwordEncoder;
    private MerchantApiKeyService apiKeyService;

    @BeforeEach
    void setUp() {
        apiKeyRepository = mock(MerchantApiKeyRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        apiKeyService = new MerchantApiKeyService(apiKeyRepository, passwordEncoder);
    }

    @Test
    void validateApiKey_WithValidKey_ShouldReturnOptionalWithKey() {
        String rawKey = "valid-api-key-12345";
        MerchantApiKey key = MerchantApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .keyHash(passwordEncoder.encode(rawKey))
                .keyPrefix("key_")
                .isActive(true)
                .scopes(Map.of("payments:read", true, "payments:write", true))
                .build();

        when(apiKeyRepository.findActiveByKeyHash(anyString())).thenReturn(Optional.of(key));

        Optional<MerchantApiKey> result = apiKeyService.validateApiKey(rawKey);
        assertTrue(result.isPresent());
    }

    @Test
    void validateApiKey_WithNullKey_ShouldReturnEmpty() {
        Optional<MerchantApiKey> result = apiKeyService.validateApiKey(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void validateApiKey_WithShortKey_ShouldReturnEmpty() {
        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("short");
        assertTrue(result.isEmpty());
    }

    @Test
    void hasScope_WithMatchingScope_ShouldReturnTrue() {
        String rawKey = "valid-api-key-12345";
        MerchantApiKey key = MerchantApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .keyHash(passwordEncoder.encode(rawKey))
                .keyPrefix("key_")
                .isActive(true)
                .scopes(Map.of("payments:read", true))
                .build();

        when(apiKeyRepository.findActiveByKeyHash(anyString())).thenReturn(Optional.of(key));

        assertTrue(apiKeyService.hasScope(rawKey, "payments:read"));
    }

    @Test
    void hasScope_WithNonMatchingScope_ShouldReturnFalse() {
        String rawKey = "valid-api-key-12345";
        MerchantApiKey key = MerchantApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .keyHash(passwordEncoder.encode(rawKey))
                .keyPrefix("key_")
                .isActive(true)
                .scopes(Map.of("payments:read", true))
                .build();

        when(apiKeyRepository.findActiveByKeyHash(anyString())).thenReturn(Optional.of(key));

        assertFalse(apiKeyService.hasScope(rawKey, "payments:write"));
    }

    @Test
    void isIpAllowed_WithEmptyWhitelist_ShouldReturnTrue() {
        String rawKey = "valid-api-key-12345";
        MerchantApiKey key = MerchantApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .keyHash(passwordEncoder.encode(rawKey))
                .keyPrefix("key_")
                .isActive(true)
                .ipWhitelist(null)
                .build();

        when(apiKeyRepository.findActiveByKeyHash(anyString())).thenReturn(Optional.of(key));

        assertTrue(apiKeyService.isIpAllowed(rawKey, "192.168.1.1"));
    }

    @Test
    void rotateApiKey_ShouldRevokeExistingKey() {
        UUID keyId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        MerchantApiKey existingKey = MerchantApiKey.builder()
                .id(keyId)
                .merchantId(merchantId)
                .keyHash("hash")
                .keyPrefix("key_")
                .isActive(true)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(existingKey));

        MerchantApiKey result = apiKeyService.rotateApiKey(keyId, merchantId);

        assertNotNull(result);
        assertNotNull(result.getRevokedAt());
        assertEquals("Rotated", result.getRevokedReason());
        assertFalse(result.getIsActive());
        verify(apiKeyRepository, times(1)).save(existingKey);
    }

    @Test
    void revokeAPIKey_ShouldSetRevokedFields() {
        UUID keyId = UUID.randomUUID();
        UUID requestedBy = UUID.randomUUID();
        String reason = "Security compromise";

        MerchantApiKey key = MerchantApiKey.builder()
                .id(keyId)
                .merchantId(UUID.randomUUID())
                .keyHash("hash")
                .keyPrefix("key_")
                .isActive(true)
                .build();

        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(key));

        MerchantApiKey result = apiKeyService.revokeAPIKey(keyId, requestedBy, reason);

        assertNotNull(result);
        assertEquals(reason, result.getRevokedReason());
        assertEquals(requestedBy, result.getRevokedBy());
        assertFalse(result.getIsActive());
        verify(apiKeyRepository, times(1)).save(key);
    }

    @Test
    void rotateApiKey_WithNonExistentKey_ShouldThrowException() {
        UUID keyId = UUID.randomUUID();
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> apiKeyService.rotateApiKey(keyId, UUID.randomUUID()));
    }

    @Test
    void getActiveKeysForMerchant_ShouldReturnKeys() {
        UUID merchantId = UUID.randomUUID();
        List<MerchantApiKey> keys = List.of(
                MerchantApiKey.builder().id(UUID.randomUUID()).merchantId(merchantId).keyPrefix("key_1").isActive(true).build(),
                MerchantApiKey.builder().id(UUID.randomUUID()).merchantId(merchantId).keyPrefix("key_2").isActive(true).build()
        );

        when(apiKeyRepository.findActiveByMerchantId(merchantId)).thenReturn(keys);

        List<MerchantApiKey> result = apiKeyService.getActiveKeysForMerchant(merchantId);
        assertEquals(2, result.size());
    }
}
