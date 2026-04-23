package dev.payment.paymentservice.payment.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ServiceAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(ServiceAuthenticationService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_VALIDITY_SECONDS = 60;

    private final String serviceName;
    private final String signingSecret;

    public ServiceAuthenticationService(
            @Value("${spring.application.name}") String serviceName,
            @Value("${application.internal-auth.signing-secret:}") String signingSecret
    ) {
        this.serviceName = serviceName;
        this.signingSecret = signingSecret != null && !signingSecret.isBlank() ? signingSecret : generateDefault();
    }

    public String generateServiceToken(String targetService, Map<String, String> claims) {
        long expiresAt = Instant.now().getEpochSecond() + TOKEN_VALIDITY_SECONDS;

        TreeMap<String, String> payload = new TreeMap<>();
        payload.put("iss", serviceName);
        payload.put("aud", targetService);
        payload.put("exp", String.valueOf(expiresAt));
        payload.put("iat", String.valueOf(Instant.now().getEpochSecond()));
        payload.put("nonce", generateNonce());
        if (claims != null) {
            payload.putAll(claims);
        }

        String payloadStr = mapToString(payload);
        String signature = sign(payloadStr);

        TreeMap<String, String> token = new TreeMap<>();
        token.put("payload", payloadStr);
        token.put("sig", signature);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                mapToString(token).getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean validateServiceToken(String token, String expectedIssuer) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            Map<String, String> parts = parseString(decoded);
            String payloadStr = parts.get("payload");
            String signature = parts.get("sig");

            if (payloadStr == null || signature == null) {
                log.warn("event=invalid_token_format token={}", token.substring(0, Math.min(20, token.length())));
                return false;
            }

            if (!verifySignature(payloadStr, signature)) {
                log.warn("event=invalid_signature");
                return false;
            }

            Map<String, String> payload = parseString(payloadStr);

            String issuer = payload.get("iss");
            String audience = payload.get("aud");
            String expiresStr = payload.get("exp");

            if (issuer == null || !issuer.equals(expectedIssuer)) {
                log.warn("event=invalid_issuer expected={} actual={}", expectedIssuer, issuer);
                return false;
            }

            if (audience == null || !audience.equals(serviceName)) {
                log.warn("event=invalid_audience expected={} actual={}", serviceName, audience);
                return false;
            }

            long expires = Long.parseLong(expiresStr);
            if (Instant.now().getEpochSecond() > expires) {
                log.warn("event=token_expired expires={}", expires);
                return false;
            }

            log.debug("event=token_validated issuer={} audience={}", issuer, audience);
            return true;
        } catch (Exception e) {
            log.error("event=token_validation_error error={}", e.getMessage());
            return false;
        }
    }

    public String getServiceName() {
        return serviceName;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }

    private boolean verifySignature(String payload, String signature) {
        String expected = sign(payload);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String mapToString(TreeMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) sb.append('&');
            sb.append(k).append('=').append(v);
        });
        return sb.toString();
    }

    private Map<String, String> parseString(String str) {
        TreeMap<String, String> map = new TreeMap<>();
        for (String pair : str.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                map.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return map;
    }

    private String generateNonce() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.valueOf(System.nanoTime()).getBytes(StandardCharsets.UTF_8)
        ).substring(0, 16);
    }

    private String generateDefault() {
        log.warn("event=using_default_signing_secret DO NOT USE IN PRODUCTION");
        return "payflow-internal-service-signing-secret-change-in-prod";
    }
}