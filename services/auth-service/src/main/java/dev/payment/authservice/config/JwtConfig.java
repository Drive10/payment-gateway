package dev.payment.authservice.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNoLXBheW1lbnQta2V5LWJhc2U2NC1lbmNvZGVk";
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
