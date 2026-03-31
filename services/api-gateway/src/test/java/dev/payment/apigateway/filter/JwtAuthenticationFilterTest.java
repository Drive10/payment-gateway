package dev.payment.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.apigateway.security.GatewayJwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private GatewayFilterChain filterChain;

    private GatewayJwtService gatewayJwtService;
    private ObjectMapper objectMapper;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private String testSecretKey;

    @BeforeEach
    void setUp() {
        testSecretKey = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=";
        gatewayJwtService = new GatewayJwtService(testSecretKey);
        objectMapper = new ObjectMapper();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(gatewayJwtService, objectMapper);
    }

    @Test
    void filter_PublicPath_PassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
    }

    @Test
    void filter_Actuator_PassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
    }

    @Test
    void filter_MissingAuthToken_ReturnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/payments").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
        
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_InvalidAuthToken_ReturnsUnauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
        
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_ValidToken_PassesThrough() {
        String token = createToken("testuser", List.of("USER"), "access");
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
        
        assertNull(exchange.getResponse().getStatusCode());
        assertEquals("testuser", exchange.getRequest().getHeaders().getFirst("X-Authenticated-User"));
    }

    @Test
    void filter_AdminPathWithoutAdminRole_ReturnsForbidden() {
        String token = createToken("testuser", List.of("USER"), "access");
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
        
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_AdminPathWithAdminRole_PassesThrough() {
        String token = createToken("adminuser", List.of("ROLE_ADMIN"), "access");
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
        
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void filter_WebhooksPath_PassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/webhooks/stripe")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                .verifyComplete();
    }

    private String createToken(String subject, List<String> roles, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("token_type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
