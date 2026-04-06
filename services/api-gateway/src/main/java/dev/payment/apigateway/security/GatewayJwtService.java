package dev.payment.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;
import java.util.Map;

@Service
public class GatewayJwtService {

    private final SecretKey secretKey;

    public GatewayJwtService(@Value("${security.jwt.secret-key}") String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    public GatewayPrincipal validateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object tokenType = claims.get("token_type");
        if (tokenType != null && !"access".equals(tokenType.toString())) {
            throw new IllegalArgumentException("Unsupported token type");
        }

        List<String> roles = claims.get("roles", List.class);
        return new GatewayPrincipal(claims.getSubject(), roles == null ? List.of() : roles);
    }

    public record GatewayPrincipal(String username, List<String> roles) {
        public boolean hasRole(String roleName) {
            return roles.stream().anyMatch(role -> role.equals(roleName) || role.equals("ROLE_" + roleName));
        }

        public Map<String, Object> asHeaders() {
            Map<String, Object> headers = new java.util.HashMap<>();
            headers.put("X-Authenticated-User", username);
            headers.put("X-Authenticated-Roles", String.join(",", roles));
            return headers;
        }
    }
}
