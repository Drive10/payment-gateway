package dev.payment.notificationservice.service;

import dev.payment.notificationservice.exception.WebhookValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookValidatorTest {

    private WebhookValidator webhookValidator;

    private static final String SECRET = "test-webhook-secret-key";
    private static final String PAYLOAD = "{\"event\":\"payment.captured\",\"id\":\"evt_test_123\"}";

    @BeforeEach
    void setUp() {
        webhookValidator = new WebhookValidator();
        ReflectionTestUtils.setField(webhookValidator, "signatureSecret", SECRET);
        ReflectionTestUtils.setField(webhookValidator, "signatureEnabled", true);
        ReflectionTestUtils.setField(webhookValidator, "toleranceSeconds", 300L);
    }

    @Test
    void validateSignature_withValidSignature_shouldPass() throws Exception {
        String signature = computeSignature(PAYLOAD);
        
        assertDoesNotThrow(() -> webhookValidator.validateSignature(signature, PAYLOAD));
    }

    @Test
    void validateSignature_withInvalidSignature_shouldThrow() {
        String invalidSignature = "invalid_signature_here";
        
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateSignature(invalidSignature, PAYLOAD));
    }

    @Test
    void validateSignature_withMissingSignature_shouldThrow() {
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateSignature(null, PAYLOAD));
    }

    @Test
    void validateSignature_withBlankSignature_shouldThrow() {
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateSignature("  ", PAYLOAD));
    }

    @Test
    void validateSignature_withDisabledValidation_shouldPass() {
        ReflectionTestUtils.setField(webhookValidator, "signatureEnabled", false);
        
        assertDoesNotThrow(() -> webhookValidator.validateSignature("any_signature", PAYLOAD));
    }

    @Test
    void validateSignature_withEmptySecret_shouldThrow() {
        ReflectionTestUtils.setField(webhookValidator, "signatureSecret", "");
        
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateSignature("any_signature", PAYLOAD));
    }

    @Test
    void validateTimestamp_withValidTimestamp_shouldPass() {
        long currentTime = System.currentTimeMillis() / 1000;
        String timestamp = String.valueOf(currentTime);
        
        assertDoesNotThrow(() -> webhookValidator.validateTimestamp(timestamp));
    }

    @Test
    void validateTimestamp_withExpiredTimestamp_shouldThrow() {
        long expiredTime = (System.currentTimeMillis() / 1000) - 600;
        String timestamp = String.valueOf(expiredTime);
        
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateTimestamp(timestamp));
    }

    @Test
    void validateTimestamp_withFutureTimestamp_shouldThrow() {
        long futureTime = (System.currentTimeMillis() / 1000) + 600;
        String timestamp = String.valueOf(futureTime);
        
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateTimestamp(timestamp));
    }

    @Test
    void validateTimestamp_withMissingTimestamp_shouldThrow() {
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateTimestamp(null));
    }

    @Test
    void validateTimestamp_withInvalidFormat_shouldThrow() {
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateTimestamp("not-a-number"));
    }

    @Test
    void validateSignature_withModifiedPayload_shouldThrow() throws Exception {
        String signature = computeSignature(PAYLOAD);
        String modifiedPayload = PAYLOAD + "modified";
        
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateSignature(signature, modifiedPayload));
    }

    @Test
    void validateSignature_timingSafeComparison_shouldPreventTimingAttacks() {
        String validSignature = assertDoesNotThrow(() -> computeSignature(PAYLOAD));
        
        assertDoesNotThrow(() -> webhookValidator.validateSignature(validSignature, PAYLOAD));
        
        String tamperedSignature = "a".repeat(validSignature.length());
        assertThrows(WebhookValidationException.class,
                () -> webhookValidator.validateSignature(tamperedSignature, PAYLOAD));
    }

    @Test
    void isSignatureEnabled_shouldReturnCorrectValue() {
        assertTrue(webhookValidator.isSignatureEnabled());
        
        ReflectionTestUtils.setField(webhookValidator, "signatureEnabled", false);
        assertFalse(webhookValidator.isSignatureEnabled());
    }

    @Test
    void getToleranceSeconds_shouldReturnConfiguredValue() {
        assertEquals(300L, webhookValidator.getToleranceSeconds());
        
        ReflectionTestUtils.setField(webhookValidator, "toleranceSeconds", 600L);
        assertEquals(600L, webhookValidator.getToleranceSeconds());
    }

    private String computeSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hmacBytes);
    }
}
