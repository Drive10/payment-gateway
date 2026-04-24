package dev.payment.merchantbackend.service;

import dev.payment.merchantbackend.dto.AuthResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final Map<String, String> refreshTokens = new ConcurrentHashMap<>();

    public AuthService(
            @Value("${security.jwt.secret-key}") String secretKey,
            @Value("${security.jwt.expiration:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public AuthResponse login(String email, String password) {
        return generateToken(email);
    }

    public AuthResponse register(String email, String password, String firstName, String lastName) {
        return generateToken(email);
    }

    public AuthResponse refreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

            String email = claims.getSubject();
            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new IllegalArgumentException("Invalid token type");
            }

            return generateToken(email);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokens.remove(refreshToken);
        }
    }

    private AuthResponse generateToken(String email) {
        String accessToken = Jwts.builder()
            .subject(email)
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(secretKey)
            .compact();

        String refreshTokenValue = Jwts.builder()
            .subject(email)
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs * 7))
            .signWith(secretKey)
            .compact();

        refreshTokens.put(refreshTokenValue, email);

        return new AuthResponse(accessToken, refreshTokenValue, expirationMs / 1000);
    }
}