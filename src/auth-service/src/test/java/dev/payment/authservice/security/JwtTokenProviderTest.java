package dev.payment.authservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret",
                "dGhpcyBpcyBhIGp3dCBzZWNyZXQga2V5IGZvciBwYXlmbG93IGRldiBlbnZpcm9ubWVudA==");
        ReflectionTestUtils.setField(tokenProvider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshTokenExpiration", 86400000L);
    }

    @Test
    void generateAccessToken_ShouldReturnValidJwt() {
        String token = tokenProvider.generateAccessToken("test@example.com", "CUSTOMER");
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = tokenProvider.generateAccessToken("test@example.com", "CUSTOMER");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        assertFalse(tokenProvider.validateToken("invalid-token"));
    }

    @Test
    void getEmailFromToken_ShouldReturnCorrectEmail() {
        String token = tokenProvider.generateAccessToken("test@example.com", "CUSTOMER");
        assertEquals("test@example.com", tokenProvider.getEmailFromToken(token));
    }

    @Test
    void getRoleFromToken_ShouldReturnCorrectRole() {
        String token = tokenProvider.generateAccessToken("test@example.com", "MERCHANT");
        assertEquals("MERCHANT", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void generateRefreshToken_ShouldReturnValidJwt() {
        String token = tokenProvider.generateRefreshToken("test@example.com");
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void getAccessTokenExpiration_ShouldReturnConfiguredValue() {
        assertEquals(3600000L, tokenProvider.getAccessTokenExpiration());
    }
}
