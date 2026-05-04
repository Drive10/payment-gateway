package dev.payment.paymentservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
public class WebhookSecurityFilter extends OncePerRequestFilter {

    @Value("${app.webhook.secret}")
    private String webhookSecret;

    @Value("${app.webhook.max-age-seconds:300}")
    private long maxAgeSeconds;

    @Value("${app.webhook.nonce-ttl-seconds:86400}")
    private long nonceTtlSeconds;

    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";
    private static final String NONCE_HEADER = "X-Webhook-Nonce";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

    private final StringRedisTemplate redisTemplate;

    public WebhookSecurityFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!request.getRequestURI().contains("/webhooks/provider")) {
            filterChain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);

        if (signature == null || timestamp == null || nonce == null) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing security headers");
            return;
        }

        if (!isValidTimestamp(timestamp)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Timestamp expired or invalid");
            return;
        }

        if (!isValidNonce(nonce)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Nonce already used");
            return;
        }

        String body = request.getReader().lines().reduce("", String::concat);
        if (!isValidSignature(body, timestamp, nonce, signature)) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
            return;
        }

        storeNonce(nonce);
        filterChain.doFilter(request, response);
    }

    private boolean isValidTimestamp(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            return Math.abs(now - ts) <= maxAgeSeconds;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidNonce(String nonce) {
        String key = "webhook:nonce:" + nonce;
        return !Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private void storeNonce(String nonce) {
        String key = "webhook:nonce:" + nonce;
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(nonceTtlSeconds));
    }

    private boolean isValidSignature(String body, String timestamp, String nonce, String providedSignature) {
        String payload = timestamp + "." + nonce + "." + body;
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hmacBytes);
            return constantTimeEquals(computedSignature, providedSignature);
        } catch (Exception e) {
            log.error("Error computing signature: {}", e.getMessage());
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\"}");
    }
}