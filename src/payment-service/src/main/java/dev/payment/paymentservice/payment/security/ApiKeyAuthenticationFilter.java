package dev.payment.paymentservice.payment.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final String validApiKey;
    private final String paymentServiceUrl;

    public ApiKeyAuthenticationFilter(
            @Value("${payment-service.api-key:sk_test_merchant123}") String validApiKey,
            @Value("${payment-service.url:http://localhost:8083}") String paymentServiceUrl) {
        this.validApiKey = validApiKey;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String apiKey = authHeader.substring(7);
            
            if (apiKey.equals(validApiKey)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "merchant",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_MERCHANT"))
                );
                auth.setDetails(new ApiKeyAuthenticationDetails(apiKey));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") 
            || path.startsWith("/internal")
            || path.equals("/auth/login")
            || path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/payments/initiate")
            || path.equals("/payments/initiate")
            || path.equals("/api/v1/payments/initiate");
    }

    public static class ApiKeyAuthenticationDetails {
        private final String apiKey;

        public ApiKeyAuthenticationDetails(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiKey() {
            return apiKey;
        }
    }
}