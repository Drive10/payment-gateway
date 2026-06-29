package dev.payment.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private GatewayFilterChain filterChain;
    private JwtAuthenticationFilter filter;

    private static final String JWT_SECRET = "dGhpcyBpcyBhIGp3dCBzZWNyZXQga2V5IGZvciBwYXlmbG93IGRldiBlbnZpcm9ubWVudA==";
    private static final String INTERNAL_SECRET = "internal_service_secret_for_payflow_dev_environment_123456789";

    @BeforeEach
    void setUp() {
        filterChain = mock(GatewayFilterChain.class);
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(filter, "internalAuthSecret", INTERNAL_SECRET);
        ReflectionTestUtils.setField(filter, "serviceName", "api-gateway");
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void filter_PublicPath_ShouldAllowWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    @Test
    void filter_PublicApiV1Path_ShouldAllowWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    @Test
    void filter_PublicActuatorPath_ShouldAllowWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    @Test
    void filter_ProtectedPath_WithoutAuthHeader_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/create-order").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        ServerHttpResponse response = exchange.getResponse();

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void filter_ProtectedPath_WithInvalidToken_ShouldReturnUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/create-order")
                .header("Authorization", "Bearer invalid-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);
        ServerHttpResponse response = exchange.getResponse();

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(filterChain, never()).filter(any());
    }

    @Test
    void filter_ProtectedPath_WithValidToken_ShouldForwardWithInternalHeaders() {
        String token = generateValidJwt("user@example.com", "CUSTOMER");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/create-order")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(captor.capture());
        ServerWebExchange modifiedExchange = captor.getValue();
        ServerHttpRequest modifiedRequest = modifiedExchange.getRequest();
        assertEquals("user@example.com", modifiedRequest.getHeaders().getFirst("X-User-Email"));
        assertEquals("CUSTOMER", modifiedRequest.getHeaders().getFirst("X-User-Role"));
        assertNotNull(modifiedRequest.getHeaders().getFirst("X-Internal-Service-Token"));
    }

    @Test
    void filter_ProtectedPath_WithMerchantRole_ShouldForwardWithCorrectRole() {
        String token = generateValidJwt("merchant@test.com", "MERCHANT");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/payments/create-order")
                .header("Authorization", "Bearer " + token)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(captor.capture());
        ServerWebExchange modifiedExchange = captor.getValue();
        assertEquals("MERCHANT", modifiedExchange.getRequest().getHeaders().getFirst("X-User-Role"));
    }

    @Test
    void filter_ProtectedPath_WithAdminPath_ShouldAllowWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/admin/finance/reports").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    private String generateValidJwt(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
