package dev.payment.authservice.security;

import dev.payment.authservice.entity.MerchantApiKey;
import dev.payment.authservice.service.MerchantApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureApiKeyAuthFilter extends OncePerRequestFilter {

    private final MerchantApiKeyService apiKeyService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = authHeader.substring(7);

        if (rawKey.length() < 10) {
            sendUnauthorized(response, "Invalid API key format");
            return;
        }

        try {
            String keyHash = hashKeyConstantTime(rawKey);

            apiKeyService.findByKey(rawKey).ifPresentOrElse(
                    key -> {
                        if (!constantTimeEquals(key.getKeyHash(), keyHash)) {
                            log.warn("API key hash mismatch for prefix: {}", key.getKeyPrefix());
                            SecurityContextHolder.clearContext();
                            return;
                        }

                        if (!key.getIsActive()) {
                            log.warn("API key is inactive: {}", key.getKeyPrefix());
                            SecurityContextHolder.clearContext();
                            return;
                        }

                        String clientIp = getClientIp(request);
                        if (!isIpAllowed(key, clientIp)) {
                            log.warn("IP not allowed: {} for key: {}", clientIp, key.getKeyPrefix());
                            SecurityContextHolder.clearContext();
                            return;
                        }

                        List<SimpleGrantedAuthority> authorities = key.getScopes().keySet().stream()
                                .filter(scope -> key.getScopes().get(scope))
                                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope.toUpperCase().replace(":", "_")))
                                .toList();

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                key.getMerchantId(), null, authorities);
                        auth.setDetails(key);
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        log.debug("Authenticated API key: {} for merchant: {}", key.getKeyPrefix(), key.getMerchantId());
                    },
                    () -> {
                        log.warn("Invalid API key submitted");
                    }
            );
        } catch (Exception e) {
            log.error("Error validating API key: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isIpAllowed(MerchantApiKey key, String clientIp) {
        if (key.getIpWhitelist() == null || key.getIpWhitelist().isEmpty()) {
            return true;
        }
        return key.getIpWhitelist().containsKey(clientIp);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\",\"success\":false}");
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

    private String hashKeyConstantTime(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}