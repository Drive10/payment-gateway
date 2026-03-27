package dev.payment.ledgerservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class GatewayTrustFilter extends OncePerRequestFilter {

    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs"
    );

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String expectedSecret;

    public GatewayTrustFilter(
            ObjectMapper objectMapper,
            @Value("${application.gateway-trust.enabled:true}") boolean enabled,
            @Value("${application.gateway-trust.internal-secret:dev-gateway-internal-secret}") String expectedSecret
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || isExempt(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (expectedSecret.equals(request.getHeader("X-Gateway-Token"))) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getOutputStream().write(objectMapper.writeValueAsBytes(
                    ApiResponse.failure(new ErrorDetails("UNTRUSTED_GATEWAY", "Ledger service only accepts gateway-originated requests", null))
            ));
        } catch (Exception exception) {
            response.getOutputStream().write(
                    "{\"success\":false,\"error\":{\"code\":\"UNTRUSTED_GATEWAY\",\"message\":\"Ledger service only accepts gateway-originated requests\"}}"
                            .getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    private boolean isExempt(String path) {
        return EXEMPT_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
