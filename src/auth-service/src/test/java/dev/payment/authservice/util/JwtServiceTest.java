package dev.payment.authservice.util;

import dev.payment.authservice.config.JwtConfig;
import dev.payment.authservice.entity.Role;
import dev.payment.authservice.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTest {

    private JwtConfig jwtConfig;
    private dev.payment.authservice.service.JwtService jwtService;
    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        String secret = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLWhtYWMtc2hhLTI1Ni13aGljaC1yZXF1aXJlcy1hdC1sZWFzdC0zMi1jaGFyYWN0ZXJz";
        
        jwtConfig = mock(JwtConfig.class);
        when(jwtConfig.getSecret()).thenReturn(secret);
        when(jwtConfig.getExpiration()).thenReturn(3600L);
        when(jwtConfig.getRefreshExpiration()).thenReturn(604800L);

        jwtService = new dev.payment.authservice.service.JwtService(jwtConfig);

        testRole = new Role();
        testRole.setId(UUID.randomUUID());
        testRole.setName("ROLE_USER");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setFullName("John Doe");
        testUser.setEnabled(true);
        testUser.setActive(true);
        testUser.setRoles(Set.of(testRole));
    }

    @Test
    void generateAccessToken_ValidUser_ReturnsToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void generateAccessToken_TokenContainsCorrectEmail() {
        String token = jwtService.generateAccessToken(testUser);

        String extractedEmail = jwtService.extractEmail(token);
        assertEquals(testUser.getEmail(), extractedEmail);
    }

    @Test
    void generateAccessToken_TokenContainsAccessType() {
        String token = jwtService.generateAccessToken(testUser);

        String tokenType = jwtService.extractClaim(token, claims -> 
            claims.get("token_type", String.class));
        assertEquals("access", tokenType);
    }

    @Test
    void generateRefreshToken_ValidUser_ReturnsToken() {
        String token = jwtService.generateRefreshToken(testUser);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void generateRefreshToken_TokenContainsRefreshType() {
        String token = jwtService.generateRefreshToken(testUser);

        String tokenType = jwtService.extractClaim(token, claims -> 
            claims.get("token_type", String.class));
        assertEquals("refresh", tokenType);
    }

    @Test
    void extractEmail_ValidToken_ReturnsEmail() {
        String token = jwtService.generateAccessToken(testUser);

        String email = jwtService.extractEmail(token);

        assertEquals(testUser.getEmail(), email);
    }

    @Test
    void extractEmail_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.here";

        assertThrows(JwtException.class, () -> jwtService.extractEmail(invalidToken));
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtService.generateAccessToken(testUser);

        boolean result = jwtService.validateToken(token);

        assertTrue(result);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";

        boolean result = jwtService.validateToken(invalidToken);

        assertFalse(result);
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxNjAwMDAwMDAwfQ.invalid";

        boolean result = jwtService.validateToken(expiredToken);

        assertFalse(result);
    }

    @Test
    void isTokenExpired_ValidToken_ReturnsFalse() {
        String token = jwtService.generateAccessToken(testUser);

        boolean result = jwtService.isTokenExpired(token);

        assertFalse(result);
    }

    @Test
    void getAccessTokenExpiration_ReturnsConfiguredValue() {
        long expiration = jwtService.getAccessTokenExpiration();

        assertEquals(3600L, expiration);
    }

    @Test
    void getRefreshTokenExpiration_ReturnsConfiguredValue() {
        long expiration = jwtService.getRefreshTokenExpiration();

        assertEquals(604800L, expiration);
    }

    @Test
    void generateAccessToken_MultipleUsers_SameSecret_DifferentTokens() {
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setEmail("user1@example.com");
        user1.setPasswordHash("hash");
        user1.setFirstName("User");
        user1.setLastName("One");
        user1.setFullName("User One");
        user1.setEnabled(true);
        user1.setActive(true);
        user1.setRoles(Set.of(testRole));

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hash");
        user2.setFirstName("User");
        user2.setLastName("Two");
        user2.setFullName("User Two");
        user2.setEnabled(true);
        user2.setActive(true);
        user2.setRoles(Set.of(testRole));

        String token1 = jwtService.generateAccessToken(user1);
        String token2 = jwtService.generateAccessToken(user2);

        assertNotEquals(token1, token2);
        assertEquals("user1@example.com", jwtService.extractEmail(token1));
        assertEquals("user2@example.com", jwtService.extractEmail(token2));
    }

    @Test
    void extractClaim_CustomClaim_ReturnsValue() {
        String token = jwtService.generateAccessToken(testUser);

        String tokenType = jwtService.extractClaim(token, claims -> 
            claims.get("token_type", String.class));

        assertNotNull(tokenType);
    }
}