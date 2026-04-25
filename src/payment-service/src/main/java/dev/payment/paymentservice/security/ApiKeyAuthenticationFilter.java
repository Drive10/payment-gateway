package dev.payment.paymentservice.security;

import dev.payment.paymentservice.client.MerchantAuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private final MerchantAuthClient authClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String userEmail = request.getHeader("X-User-Email");
        String userRole = request.getHeader("X-User-Role");
        
        if (userEmail != null && userRole != null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userEmail, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userRole.toUpperCase())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user from gateway: {} with role {}", userEmail, userRole);
            filterChain.doFilter(request, response);
            return;
        }
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = authHeader.substring(7);
        Map<String, Object> merchantInfo = authClient.validateApiKey(apiKey);

        if (merchantInfo != null) {
            String merchantId = (String) merchantInfo.get("merchantId");
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    merchantId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_MERCHANT")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated merchant: {}", merchantId);
        }

        filterChain.doFilter(request, response);
    }
}
