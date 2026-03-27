package dev.payment.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayJwtServiceTest {

    private static final String SECRET = "VGhpc0lzQVN0cm9uZ0Jhc2U2NEVuY29kZWRKU1dUU2VjcmV0S2V5Rm9yRmluVGVjaERldg==";

    @Test
    void shouldAcceptAccessTokenAndExposeRoles() {
        GatewayJwtService service = new GatewayJwtService(SECRET);
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("user@example.com")
                .claim("roles", List.of("ROLE_USER"))
                .claim("token_type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();

        GatewayJwtService.GatewayPrincipal principal = service.validateAccessToken(token);

        assertThat(principal.username()).isEqualTo("user@example.com");
        assertThat(principal.hasRole("USER")).isTrue();
    }
}
