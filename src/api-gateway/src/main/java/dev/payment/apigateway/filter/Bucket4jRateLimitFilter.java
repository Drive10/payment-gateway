package dev.payment.apigateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Bucket4jRateLimitFilter implements GlobalFilter, Ordered {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final int REQUESTS_PER_SECOND = 100;
    private static final int BURST_CAPACITY = 20;

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator",
            "/swagger-ui",
            "/swagger-ui.html",
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

        String key = resolveKey(exchange);
        Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);

        if (bucket.tryConsume(1)) {
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(REQUESTS_PER_SECOND));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", "1");
        return exchange.getResponse().setComplete();
    }

    private Bucket createBucket(String key) {
        Bandwidth limit = Bandwidth.classic(
                BURST_CAPACITY,
                Refill.greedy(REQUESTS_PER_SECOND, Duration.ofSeconds(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    private boolean isPublic(String path) {
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private String resolveKey(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return "user_" + token.hashCode();
        }

        String xUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (xUserId != null) {
            return "user_" + xUserId;
        }

        var remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String hostAddress = remoteAddress.getAddress().getHostAddress();
            if (hostAddress != null) {
                return "ip_" + hostAddress;
            }
        }

        return "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
