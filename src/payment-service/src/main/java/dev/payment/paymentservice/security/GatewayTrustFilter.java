package dev.payment.paymentservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GatewayTrustFilter.class);

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Token";

    private static final List<String> EXEMPT_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/api/v1/auth",
            "/api/v1/webhooks",
            "/internal/",
            "/payments/initiate",
            "/payments/initiate-compat",
            "/payments/tokenize",
            "/internal/platform/",
            "/orders/",
            "/payments/status"
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
        log.info("GatewayTrustFilter initialized: enabled={}, hasSecret={}", enabled, expectedSecret != null && !expectedSecret.isEmpty());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        log.debug("GatewayTrustFilter processing: path={}, enabled={}, isExempt={}", path, enabled, isExempt(path));

        if (!enabled || isExempt(path)) {
            log.debug("GatewayTrustFilter: passing through (enabled={}, exempt={})", enabled, isExempt(path));
            filterChain.doFilter(request, response);
            return;
        }

        String receivedSecret = request.getHeader(GATEWAY_SECRET_HEADER);
        log.debug("GatewayTrustFilter: checking secret, expectedSecret={}, receivedSecret={}", expectedSecret != null, receivedSecret != null);

        if (expectedSecret != null && expectedSecret.equals(receivedSecret)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("GatewayTrustFilter: blocking request - invalid or missing gateway token for path={}", path);
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