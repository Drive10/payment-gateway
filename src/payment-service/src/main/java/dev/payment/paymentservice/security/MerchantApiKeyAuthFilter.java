package dev.payment.paymentservice.security;

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

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MerchantApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${app.merchant.api-keys:}")
    private String merchantApiKeys;

    @Value("${app.internal-auth.secret}")
    private String internalAuthSecret;

    private static final String INTERNAL_SERVICE_HEADER = "X-Internal-Service-Token";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        if (SecurityContextHolder.getContext().getAuthentication() != null && 
            SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String apiKey = authHeader.substring(7);
            
            List<String> validKeys = parseApiKeys(merchantApiKeys);
            
            for (String validKey : validKeys) {
                if (validKey.equals(apiKey)) {
                    String merchantId = extractMerchantId(validKey);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            merchantId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated merchant via API key: {}", merchantId);
                    break;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
    
    private List<String> parseApiKeys(String keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(keys.split(","));
    }
    
    private String extractMerchantId(String apiKey) {
        return "merchant-" + Math.abs(apiKey.hashCode() % 10000);
    }
}