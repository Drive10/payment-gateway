package dev.payment.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.apigateway.security.GatewayJwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @Test
    void shouldRejectMissingBearerTokenForProtectedRoute() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(GatewayJwtService.class), new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/payments").build()
        );

        filter.filter(exchange, passthroughChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("401 UNAUTHORIZED");
    }

    @Test
    void shouldRejectAdminRouteWhenTokenHasNoAdminRole() {
        GatewayJwtService gatewayJwtService = mock(GatewayJwtService.class);
        when(gatewayJwtService.validateAccessToken("token-123"))
                .thenReturn(new GatewayJwtService.GatewayPrincipal("user@example.com", List.of("ROLE_USER")));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(gatewayJwtService, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/admin/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-123")
                        .build()
        );

        filter.filter(exchange, passthroughChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("403 FORBIDDEN");
    }

    @Test
    void shouldPropagateAuthenticatedHeadersForValidToken() {
        GatewayJwtService gatewayJwtService = mock(GatewayJwtService.class);
        when(gatewayJwtService.validateAccessToken("token-456"))
                .thenReturn(new GatewayJwtService.GatewayPrincipal("admin@example.com", List.of("ROLE_ADMIN", "ROLE_USER")));

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(gatewayJwtService, new ObjectMapper());
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        AtomicReference<String> authenticatedUser = new AtomicReference<>();
        AtomicReference<String> authenticatedRoles = new AtomicReference<>();
        WebFilterChain chain = exchange -> {
            chainInvoked.set(true);
            authenticatedUser.set(exchange.getRequest().getHeaders().getFirst("X-Authenticated-User"));
            authenticatedRoles.set(exchange.getRequest().getHeaders().getFirst("X-Authenticated-Roles"));
            return Mono.empty();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-456")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertThat(chainInvoked).isTrue();
        assertThat(authenticatedUser.get()).isEqualTo("admin@example.com");
        assertThat(authenticatedRoles.get()).contains("ROLE_ADMIN");
    }

    private WebFilterChain passthroughChain() {
        return exchange -> Mono.empty();
    }
}
