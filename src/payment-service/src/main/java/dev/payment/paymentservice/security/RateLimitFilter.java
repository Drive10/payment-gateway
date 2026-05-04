package dev.payment.paymentservice.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.requests-per-second:100}")
    private int requestsPerSecond;

    @Value("${app.rate-limit.burst-capacity:200}")
    private int burstCapacity;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.contains("/actuator") || path.contains("/swagger")) {
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = resolveRateLimitKey(request);
        Bucket bucket = resolveBucket(rateLimitKey);

        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(burstCapacity));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {}", rateLimitKey);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"success\":false,\"error\":\"Rate limit exceeded\",\"retryAfterSeconds\":60}"
            );
        }
    }

    private String resolveRateLimitKey(HttpServletRequest request) {
        String merchantId = resolveMerchantId(request);
        String clientIp = getClientIp(request);
        String endpoint = request.getRequestURI();

        if (merchantId != null) {
            return "rate:" + merchantId + ":" + endpoint;
        }
        return "rate:ip:" + clientIp + ":" + endpoint;
    }

    private String resolveMerchantId(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return request.getUserPrincipal().getName();
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket resolveBucket(String key) {
        return localBuckets.computeIfAbsent(key, k -> createBucket());
    }

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(burstCapacity,
                        Refill.greedy(requestsPerSecond, Duration.ofSeconds(1))))
                .build();
    }
}