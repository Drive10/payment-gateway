package dev.payment.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Multi-tenant rate limiting filter.
 * Limits requests per tenant (identified by X-Tenant-ID header or API key).
 * Default: 100 requests/minute per tenant.
 * Can be overridden per tenant via Redis config.
 */
@Component
public class TenantRateLimitingFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "ratelimit:tenant:";
    private static final long DEFAULT_LIMIT = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    public TenantRateLimitingFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/v1/auth",
            "/api/v1/webhooks"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        String tenantId = extractTenantId(exchange);
        if (tenantId == null) {
            return chain.filter(exchange);
        }

        String key = RATE_LIMIT_PREFIX + tenantId;

        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(key, WINDOW).thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    long limit = getTenantLimit(tenantId).blockOptional().orElse(DEFAULT_LIMIT);
                    if (count > limit) {
                        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
                        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(limit - count));
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> {
                    // If Redis is down, allow request but log warning
                    return chain.filter(exchange);
                });
    }

    private String extractTenantId(ServerWebExchange exchange) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        if (tenantId != null) return tenantId;

        // Extract from API key header
        String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (apiKey != null) {
            return apiKey.split("_")[0]; // tenantId_keyId format
        }

        return null;
    }

    private Mono<Long> getTenantLimit(String tenantId) {
        return redisTemplate.opsForValue().get("tenant:limit:" + tenantId)
                .map(Long::parseLong)
                .onErrorReturn(DEFAULT_LIMIT);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -2; // Run before JWT filter
    }
}
