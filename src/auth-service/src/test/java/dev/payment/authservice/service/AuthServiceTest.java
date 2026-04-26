package dev.payment.authservice.service;

import dev.payment.authservice.dto.TokenResponse;
import dev.payment.authservice.entity.Merchant;
import dev.payment.authservice.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    private AuthService authService;

    private Merchant merchant;

    @BeforeEach
    void setUp() {
        authService = new AuthService(null, null, null, merchantRepository, null, null);

        merchant = Merchant.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .businessName("Test Business")
                .apiKey("sk_test_" + UUID.randomUUID().toString().replace("-", ""))
                .webhookUrl("https://example.com/webhook")
                .status(Merchant.MerchantStatus.ACTIVE)
                .build();
    }

    @Test
    void validateApiKey_ValidActiveKey_ShouldReturnMerchant() {
        when(merchantRepository.findByApiKey(merchant.getApiKey())).thenReturn(Optional.of(merchant));

        Merchant result = authService.validateApiKey(merchant.getApiKey());

        assertNotNull(result);
        assertEquals(merchant.getApiKey(), result.getApiKey());
    }

    @Test
    void validateApiKey_InvalidKey_ShouldThrowException() {
        when(merchantRepository.findByApiKey("invalid_key")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.validateApiKey("invalid_key"));
    }

    @Test
    void getApiKeyForMerchant_ExistingMerchant_ShouldReturnApiKey() {
        when(merchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));

        String result = authService.getApiKeyForMerchant(merchant.getId().toString(), "jwt-token");

        assertEquals(merchant.getApiKey(), result);
    }

    @Test
    void getApiKeyForMerchant_NonExistingMerchant_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        when(merchantRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            authService.getApiKeyForMerchant(nonExistingId.toString(), "jwt-token"));
    }

    @Test
    void getMerchantDetails_ExistingMerchant_ShouldReturnMerchant() {
        when(merchantRepository.findById(merchant.getId())).thenReturn(Optional.of(merchant));

        Merchant result = authService.getMerchantDetails(merchant.getId().toString(), "jwt-token");

        assertNotNull(result);
        assertEquals(merchant.getId(), result.getId());
    }

    @Test
    void getMerchantDetails_NonExistingMerchant_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        when(merchantRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> 
            authService.getMerchantDetails(nonExistingId.toString(), "jwt-token"));
    }
}