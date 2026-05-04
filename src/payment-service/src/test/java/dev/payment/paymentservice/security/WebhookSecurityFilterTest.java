package dev.payment.paymentservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookSecurityFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private WebhookSecurityFilter filter;
    private String webhookSecret = "test_webhook_secret_123456789";

    @BeforeEach
    void setUp() {
        filter = new WebhookSecurityFilter(redisTemplate);
        setField(filter, "webhookSecret", webhookSecret);
        setField(filter, "maxAgeSeconds", 300L);
        setField(filter, "nonceTtlSeconds", 86400L);
    }

    @Test
    @DisplayName("Valid timestamp should be accepted")
    void isValidTimestamp_ValidTimestamp_ReturnsTrue() {
        long validTimestamp = Instant.now().getEpochSecond();
        assertTrue(filter.isValidTimestamp(String.valueOf(validTimestamp)));
    }

    @Test
    @DisplayName("Expired timestamp should be rejected")
    void isValidTimestamp_ExpiredTimestamp_ReturnsFalse() {
        long expiredTimestamp = Instant.now().getEpochSecond() - 600;
        assertFalse(filter.isValidTimestamp(String.valueOf(expiredTimestamp)));
    }

    @Test
    @DisplayName("Future timestamp should be rejected")
    void isValidTimestamp_FutureTimestamp_ReturnsFalse() {
        long futureTimestamp = Instant.now().getEpochSecond() + 600;
        assertFalse(filter.isValidTimestamp(String.valueOf(futureTimestamp)));
    }

    @Test
    @DisplayName("Non-numeric timestamp should be rejected")
    void isValidTimestamp_NonNumeric_ReturnsFalse() {
        assertFalse(filter.isValidTimestamp("not-a-number"));
    }

    @Test
    @DisplayName("Valid nonce should be accepted")
    void isValidNonce_ValidNonce_ReturnsTrue() {
        when(redisTemplate.hasKey("webhook:nonce:test_nonce_123")).thenReturn(false);
        assertTrue(filter.isValidNonce("test_nonce_123"));
    }

    @Test
    @DisplayName("Used nonce should be rejected (replay attack)")
    void isValidNonce_UsedNonce_ReturnsFalse() {
        when(redisTemplate.hasKey("webhook:nonce:replay_nonce")).thenReturn(true);
        assertFalse(filter.isValidNonce("replay_nonce"));
    }

    @Test
    @DisplayName("Invalid signature should be rejected")
    void isValidSignature_WrongSignature_ReturnsFalse() {
        String body = "{\"event\":\"payment.completed\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "test_nonce_456";
        String wrongSignature = "wrong_signature_here";

        assertFalse(filter.isValidSignature(body, timestamp, nonce, wrongSignature));
    }

    @Test
    @DisplayName("Correct HMAC signature should be accepted")
    void isValidSignature_CorrectSignature_ReturnsTrue() {
        String body = "{\"event\":\"payment.completed\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "test_nonce_789";

        String validSignature = computeHmac(body, timestamp, nonce);

        assertTrue(filter.isValidSignature(body, timestamp, nonce, validSignature));
    }

    @Test
    @DisplayName("Tampered body should be rejected")
    void isValidSignature_TamperedBody_ReturnsFalse() {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "test_nonce_tampered";

        String validSignature = computeHmac("original body", timestamp, nonce);

        assertFalse(filter.isValidSignature("tampered body", timestamp, nonce, validSignature));
    }

    @Test
    @DisplayName("Old timestamp should be rejected even with valid signature")
    void isValidSignature_OldTimestamp_ReturnsFalse() {
        String oldTimestamp = String.valueOf(Instant.now().getEpochSecond() - 400);
        String nonce = "test_nonce_old";
        String body = "{\"event\":\"test\"}";

        String validSignature = computeHmac(body, oldTimestamp, nonce);

        assertFalse(filter.isValidSignature(body, oldTimestamp, nonce, validSignature));
    }

    private String computeHmac(String body, String timestamp, String nonce) {
        try {
            String payload = timestamp + "." + nonce + "." + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}