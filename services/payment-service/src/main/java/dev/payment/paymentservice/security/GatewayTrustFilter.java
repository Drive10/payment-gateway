package dev.payment.paymentservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Order(0)
public class GatewayTrustFilter extends OncePerRequestFilter {

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Token";

    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/api/v1/auth",
            "/api/v1/webhooks",
            "/internal/",
            "/payments/initiate"
    );

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String expectedSecret;

    public GatewayTrustFilter(
            ObjectMapper objectMapper,
            @Value("${application.gateway-trust.enabled:true}") boolean enabled,
            @Value("${application.gateway-trust.internal-secret:}") String expectedSecret
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

        String receivedSecret = request.getHeader(GATEWAY_SECRET_HEADER);
        if (expectedSecret.equals(receivedSecret)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getOutputStream().write(objectMapper.writeValueAsBytes(
                    ApiResponse.failure(new ErrorDetails("UNTRUSTED_GATEWAY", "Requests must flow through the API gateway", null))
            ));
        } catch (Exception exception) {
            response.getOutputStream().write(
                    "{\"success\":false,\"error\":{\"code\":\"UNTRUSTED_GATEWAY\",\"message\":\"Requests must flow through the API gateway\"}}"
                            .getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    private boolean isExempt(String path) {
        return EXEMPT_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
