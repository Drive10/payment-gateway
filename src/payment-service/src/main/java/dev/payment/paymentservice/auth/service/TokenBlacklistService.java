package dev.payment.paymentservice.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long expirationSeconds) {
        redisTemplate.opsForValue().set(
            "token:blacklist:" + token,
            "blacklisted",
            Duration.ofSeconds(expirationSeconds)
        );
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("token:blacklist:" + token)
        );
    }
}