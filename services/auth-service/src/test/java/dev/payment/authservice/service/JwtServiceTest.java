package dev.payment.authservice.service;

import dev.payment.authservice.config.JwtConfig;
import dev.payment.authservice.entity.Role;
import dev.payment.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("testSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm");
        jwtConfig.setExpiration(3600L);
        jwtConfig.setRefreshExpiration(86400L);
        jwtService = new JwtService(jwtConfig);
    }

    private User createTestUser() {
        Role role = new Role(1L, "USER", "Regular user");
        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User(
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                "test@example.com",
                "hashedPassword",
                "John",
                "Doe",
                "1234567890",
                true
        );
        user.setRoles(roles);
        return user;
    }

    @Test
    void generateAccessToken_shouldGenerateValidToken() {
        User user = createTestUser();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    void generateAccessToken_shouldContainCorrectEmail() {
        User user = createTestUser();

        String token = jwtService.generateAccessToken(user);
        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(user.getEmail(), extractedEmail);
    }

    @Test
    void generateRefreshToken_shouldGenerateValidToken() {
        User user = createTestUser();

        String token = jwtService.generateRefreshToken(user);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3, "JWT should have 3 parts");
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        boolean isValid = jwtService.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtService.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        jwtConfig.setExpiration(-1L);
        JwtService expiredJwtService = new JwtService(jwtConfig);
        User user = createTestUser();

        String token = expiredJwtService.generateAccessToken(user);

        boolean isValid = jwtService.validateToken(token);

        assertFalse(isValid);
    }

    @Test
    void extractClaim_shouldExtractTokenType() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        String tokenType = jwtService.extractClaim(token, claims -> claims.get("type", String.class));

        assertEquals("access", tokenType);
    }

    @Test
    void extractClaim_shouldExtractRoles() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        @SuppressWarnings("unchecked")
        java.util.List<String> roles = jwtService.extractClaim(token, claims -> claims.get("roles", java.util.List.class));

        assertNotNull(roles);
        assertTrue(roles.contains("USER"));
    }

    @Test
    void isTokenExpired_shouldReturnFalseForValidToken() {
        User user = createTestUser();
        String token = jwtService.generateAccessToken(user);

        boolean isExpired = jwtService.isTokenExpired(token);

        assertFalse(isExpired);
    }

    @Test
    void getAccessTokenExpiration_shouldReturnConfiguredValue() {
        long expiration = jwtService.getAccessTokenExpiration();

        assertEquals(3600L, expiration);
    }

    @Test
    void getRefreshTokenExpiration_shouldReturnConfiguredValue() {
        long expiration = jwtService.getRefreshTokenExpiration();

        assertEquals(86400L, expiration);
    }

    @Test
    void generateTokens_shouldGenerateDifferentTokens() {
        User user = createTestUser();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        assertNotEquals(accessToken, refreshToken);
    }
}