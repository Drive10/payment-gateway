package dev.payment.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GatewayJwtServiceTest {

    private GatewayJwtService gatewayJwtService;
    private String testSecretKey;

    @BeforeEach
    void setUp() {
        testSecretKey = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk=";
        gatewayJwtService = new GatewayJwtService(testSecretKey);
    }

    @Test
    void validateAccessToken_ValidToken_ReturnsPrincipal() {
        String token = createToken("testuser", List.of("USER"), "access");

        GatewayJwtService.GatewayPrincipal principal = gatewayJwtService.validateAccessToken(token);

        assertEquals("testuser", principal.username());
        assertTrue(principal.hasRole("USER"));
    }

    @Test
    void validateAccessToken_ValidTokenWithAdminRole_ReturnsPrincipalWithAdminRole() {
        String token = createToken("adminuser", List.of("ROLE_ADMIN", "USER"), "access");

        GatewayJwtService.GatewayPrincipal principal = gatewayJwtService.validateAccessToken(token);

        assertEquals("adminuser", principal.username());
        assertTrue(principal.hasRole("ADMIN"));
    }

    @Test
    void validateAccessToken_RefreshToken_ThrowsException() {
        String token = createToken("testuser", List.of("USER"), "refresh");

        assertThrows(IllegalArgumentException.class, () ->
            gatewayJwtService.validateAccessToken(token)
        );
    }

    @Test
    void validateAccessToken_InvalidToken_ThrowsException() {
        assertThrows(Exception.class, () ->
            gatewayJwtService.validateAccessToken("invalid.token.here")
        );
    }

    @Test
    void validateAccessToken_ExpiredToken_ThrowsException() {
        String token = createExpiredToken("testuser", List.of("USER"), "access");

        assertThrows(Exception.class, () ->
            gatewayJwtService.validateAccessToken(token)
        );
    }

    @Test
    void validateAccessToken_NullRoles_ReturnsEmptyList() {
        String token = createTokenWithoutRoles("testuser", "access");

        GatewayJwtService.GatewayPrincipal principal = gatewayJwtService.validateAccessToken(token);

        assertEquals("testuser", principal.username());
        assertTrue(principal.roles().isEmpty());
    }

    @Test
    void gatewayPrincipal_hasRole_WithRolePrefix_ReturnsTrue() {
        GatewayJwtService.GatewayPrincipal principal = new GatewayJwtService.GatewayPrincipal("testuser", List.of("ROLE_ADMIN"));

        assertTrue(principal.hasRole("ADMIN"));
    }

    @Test
    void gatewayPrincipal_hasRole_WithoutRolePrefix_ReturnsTrue() {
        GatewayJwtService.GatewayPrincipal principal = new GatewayJwtService.GatewayPrincipal("testuser", List.of("ADMIN"));

        assertTrue(principal.hasRole("ADMIN"));
    }

    @Test
    void gatewayPrincipal_hasRole_NonExistentRole_ReturnsFalse() {
        GatewayJwtService.GatewayPrincipal principal = new GatewayJwtService.GatewayPrincipal("testuser", List.of("USER"));

        assertFalse(principal.hasRole("ADMIN"));
    }

    @Test
    void gatewayPrincipal_asHeaders_ReturnsCorrectHeaders() {
        GatewayJwtService.GatewayPrincipal principal = new GatewayJwtService.GatewayPrincipal("testuser", List.of("USER", "ADMIN"));

        var headers = principal.asHeaders();

        assertEquals("testuser", headers.get("X-Authenticated-User"));
        assertEquals("USER,ADMIN", headers.get("X-Authenticated-Roles"));
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

    private String createTokenWithoutRoles(String subject, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
        return Jwts.builder()
                .subject(subject)
                .claim("token_type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    private String createExpiredToken(String subject, List<String> roles, String tokenType) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKey));
        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("token_type", tokenType)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();
    }
}
