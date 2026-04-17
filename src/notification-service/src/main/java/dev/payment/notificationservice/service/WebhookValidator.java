package dev.payment.notificationservice.service;

import dev.payment.notificationservice.exception.WebhookValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class WebhookValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SHA256 = "SHA-256";

    @Value("${application.webhook.signature.secret:}")
    private String signatureSecret;

    @Value("${application.webhook.signature.enabled:true}")
    private boolean signatureEnabled;

    @Value("${application.webhook.signature.tolerance-seconds:300}")
    private long toleranceSeconds;

    public void validateSignature(String signature, String payload) {
        if (!signatureEnabled) {
            log.debug("Webhook signature validation is disabled");
            return;
        }

        if (signature == null || signature.isBlank()) {
            throw new WebhookValidationException("MISSING_SIGNATURE", "Webhook signature is required");
        }

        if (signatureSecret == null || signatureSecret.isBlank()) {
            log.error("Webhook signature secret is not configured - REJECTING webhook for security");
            throw new WebhookValidationException("CONFIGURATION_ERROR", "Webhook validation is not properly configured");
        }

        String expectedSignature = computeSignature(payload);
        if (!timingSafeEqual(signature, expectedSignature)) {
            log.warn("Webhook signature validation failed - potential attack attempt");
            throw new WebhookValidationException("INVALID_SIGNATURE", "Webhook signature validation failed");
        }

        log.debug("Webhook signature validated successfully");
    }

    public void validateTimestamp(String timestampHeader) {
        if (timestampHeader == null || timestampHeader.isBlank()) {
            throw new WebhookValidationException("MISSING_TIMESTAMP", "Webhook timestamp is required for replay protection");
        }

        try {
            long timestamp = Long.parseLong(timestampHeader);
            long currentTime = System.currentTimeMillis() / 1000;
            long timeDiff = Math.abs(currentTime - timestamp);

            if (timeDiff > toleranceSeconds) {
                log.warn("Webhook timestamp outside tolerance window: {} seconds", timeDiff);
                throw new WebhookValidationException("TIMESTAMP_EXPIRED", 
                    "Webhook timestamp is outside the acceptable tolerance window");
            }
        } catch (NumberFormatException e) {
            throw new WebhookValidationException("INVALID_TIMESTAMP", "Webhook timestamp format is invalid");
        }
    }

    private String computeSignature(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    signatureSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC signature", e);
            throw new WebhookValidationException("SIGNATURE_COMPUTATION_ERROR", 
                    "Failed to compute webhook signature");
        }
    }

    private boolean timingSafeEqual(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    public boolean isSignatureEnabled() {
        return signatureEnabled;
    }

    public long getToleranceSeconds() {
        return toleranceSeconds;
    }
}
