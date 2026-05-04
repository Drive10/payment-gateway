package dev.payment.authservice.service;

import dev.payment.authservice.entity.MerchantApiKey;
import dev.payment.authservice.repository.MerchantApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantApiKeyServiceTest {

    @Mock
    private MerchantApiKeyRepository apiKeyRepository;

    @InjectMocks
    private MerchantApiKeyService apiKeyService;

    private PasswordEncoder passwordEncoder;
    private MerchantApiKey validKey;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(12);

        validKey = MerchantApiKey.builder()
                .id(UUID.randomUUID())
                .merchantId(UUID.randomUUID())
                .keyHash(passwordEncoder.encode("test_merchant_key_123"))
                .keyPrefix("test_mer")
                .name("Test Key")
                .scopes(Map.of("payments:read", true, "payments:write", true))
                .ipWhitelist(Map.of("127.0.0.1", true))
                .isActive(true)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Valid API key should authenticate successfully")
    void validateApiKey_ValidKey_ReturnsPresent() {
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("test_merchant_key_123");

        assertTrue(result.isPresent());
        assertEquals(validKey.getMerchantId(), result.get().getMerchantId());
    }

    @Test
    @DisplayName("Invalid API key format should return empty")
    void validateApiKey_InvalidFormat_ReturnsEmpty() {
        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("short");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Null API key should return empty")
    void validateApiKey_NullKey_ReturnsEmpty() {
        Optional<MerchantApiKey> result = apiKeyService.validateApiKey(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Inactive API key should be rejected")
    void validateApiKey_InactiveKey_ReturnsEmpty() {
        validKey.setIsActive(false);
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("test_merchant_key_123");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Expired API key should be rejected")
    void validateApiKey_ExpiredKey_ReturnsEmpty() {
        validKey.setExpiresAt(Instant.now().minusSeconds(3600));
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("test_merchant_key_123");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Revoked API key should be rejected")
    void validateApiKey_RevokedKey_ReturnsEmpty() {
        validKey.setRevokedAt(Instant.now());
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("test_merchant_key_123");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Key with correct scope should have permission")
    void hasScope_ValidScope_ReturnsTrue() {
        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("test_merchant_key_123");

        assertTrue(result.isPresent());
        assertTrue(result.get().hasScope("payments:read"));
        assertTrue(result.get().hasScope("payments:write"));
    }

    @Test
    @DisplayName("Key without scope should be denied")
    void hasScope_MissingScope_ReturnsFalse() {
        Optional<MerchantApiKey> result = apiKeyService.validateApiKey("test_merchant_key_123");

        assertTrue(result.isPresent());
        assertFalse(result.get().hasScope("admin:access"));
    }

    @Test
    @DisplayName("IP whitelist should allow matching IP")
    void isIpAllowed_MatchingIP_ReturnsTrue() {
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        boolean result = apiKeyService.isIpAllowed("test_merchant_key_123", "127.0.0.1");

        assertTrue(result);
    }

    @Test
    @DisplayName("IP not in whitelist should be denied")
    void isIpAllowed_NonMatchingIP_ReturnsFalse() {
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        boolean result = apiKeyService.isIpAllowed("test_merchant_key_123", "192.168.1.1");

        assertFalse(result);
    }

    @Test
    @DisplayName("Empty whitelist should allow any IP")
    void isIpAllowed_EmptyWhitelist_AllowsAll() {
        validKey.setIpWhitelist(null);
        when(apiKeyRepository.findActiveByKeyHash(anyString()))
                .thenReturn(Optional.of(validKey));

        boolean result = apiKeyService.isIpAllowed("test_merchant_key_123", "192.168.1.1");

        assertTrue(result);
    }
}