package dev.payment.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.apigateway.security.GatewayJwtService;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/api/v1/auth",
            "/api/v1/webhooks",
            "/api/v1/payments/tokenize",
            "/api/v1/payments/initiate",
            "/api/v1/payments/initiate-compat",
            "/api/v1/payments/status",
            "/api/v1/orders"
    );

    private final GatewayJwtService gatewayJwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(GatewayJwtService gatewayJwtService, ObjectMapper objectMapper) {
        this.gatewayJwtService = gatewayJwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "MISSING_AUTH_TOKEN", "Bearer token is required");
        }

        try {
            GatewayJwtService.GatewayPrincipal principal =
                    gatewayJwtService.validateAccessToken(authorization.substring(7));

            if (path.startsWith("/api/v1/admin") && !principal.hasRole("ADMIN")) {
                return writeError(exchange, HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE", "ADMIN role is required");
            }

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();
            principal.asHeaders().forEach((name, value) -> requestBuilder.header(name, value.toString()));
            return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
        } catch (Exception exception) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "INVALID_AUTH_TOKEN", "Token is invalid or expired");
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(ApiResponse.failure(new ErrorDetails(code, message, null)));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception exception) {
            byte[] fallback = ("{\"success\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(fallback);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
