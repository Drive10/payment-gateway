package dev.payment.notificationservice.service;

import dev.payment.notificationservice.exception.WebhookValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookValidator.class);

    @Value("${application.webhook.signature.secret:}")
    private String signatureSecret;

    @Value("${application.webhook.signature.enabled:true}")
    private boolean signatureEnabled;

    public void validateSignature(String signature, String payload) {
        if (!signatureEnabled) {
            log.debug("Webhook signature validation is disabled");
            return;
        }

        if (signature == null || signature.isBlank()) {
            throw new WebhookValidationException("MISSING_SIGNATURE", "Webhook signature is required");
        }

        if (signatureSecret == null || signatureSecret.isBlank()) {
            log.warn("Webhook signature secret is not configured");
            return;
        }

        String expectedSignature = generateMockSignature(payload);
        if (!signature.equals(expectedSignature)) {
            log.warn("Webhook signature validation failed");
            throw new WebhookValidationException("INVALID_SIGNATURE", "Webhook signature validation failed");
        }
    }

    private String generateMockSignature(String payload) {
        return "mock_sig_" + payload.hashCode();
    }

    public boolean isSignatureEnabled() {
        return signatureEnabled;
    }
}
