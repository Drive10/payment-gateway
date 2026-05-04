package dev.payment.paymentservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    @Value("${app.internal-auth.secret}")
    private String internalAuthSecret;

    private static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service-Token";
    private static final List<String> TRUSTED_SERVICES = List.of("api-gateway", "auth-service", "simulator-service", "notification-service");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String internalToken = request.getHeader(INTERNAL_SERVICE_HEADER);
        
        if (internalToken != null && !internalToken.isEmpty()) {
            try {
                if (validateInternalToken(internalToken)) {
                    String serviceName = getServiceNameFromToken(internalToken);
                    if (serviceName != null && TRUSTED_SERVICES.contains(serviceName)) {
                        String userEmail = getUserEmailFromToken(internalToken);
                        String userRole = getUserRoleFromToken(internalToken);
                        
                        if (userEmail != null && userRole != null) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userEmail, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole.toUpperCase())));
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            log.debug("Authenticated internal service: {} with user {} role {}", serviceName, userEmail, userRole);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    }
                }
                log.warn("Invalid internal service token rejected");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid internal service token\"}");
                return;
            } catch (Exception e) {
                log.warn("Failed to validate internal service token: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Invalid internal service token\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean validateInternalToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(internalAuthSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    private String getServiceNameFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(internalAuthSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.get("service", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserEmailFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(internalAuthSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    private String getUserRoleFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(internalAuthSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return claims.get("role", String.class);
        } catch (Exception e) {
            return null;
        }
    }
}