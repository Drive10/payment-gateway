package dev.payment.paymentservice.auth.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application.jwt")
public class JwtConfig {
    private String secret = System.getenv("JWT_SECRET") != null ? System.getenv("JWT_SECRET") : "Q00qVqgM+A1zSb7FBfSGx6vzK9sUZhpxP5aBzWlLvp0aBcDeFgHiJkLmNoPqRsTuVwXyZaBcDeFg==";
    private long expiration = 86400000;
    private long refreshExpiration = 604800000;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    public void setRefreshExpiration(long refreshExpiration) {
        this.refreshExpiration = refreshExpiration;
    }
}
