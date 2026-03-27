package dev.payment.apigateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements WebFilter, Ordered {

    private static final String USER_AGENT_FALLBACK = "anonymous";
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        if (!requiresRateLimit(path)) {
            return chain.filter(exchange);
        }

        String key = clientKey(request);
        Bucket bucket = buckets.computeIfAbsent(key + ":" + rateClass(path), this::newBucket);
        if (bucket.tryConsume(1)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        return exchange.getResponse().setComplete();
    }

    private boolean requiresRateLimit(String path) {
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/payments")
                || path.startsWith("/api/v1/orders");
    }

    private String rateClass(String path) {
        return path.startsWith("/api/v1/auth/") ? "auth" : "payments";
    }

    private Bucket newBucket(String rateClass) {
        if (rateClass.endsWith("auth")) {
            return Bucket.builder()
                    .addLimit(Bandwidth.builder().capacity(20).refillGreedy(20, Duration.ofMinutes(1)).build())
                    .build();
        }
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(60).refillGreedy(60, Duration.ofMinutes(1)).build())
                .build();
    }

    private String clientKey(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return request.getHeaders().getFirst("User-Agent") == null
                ? USER_AGENT_FALLBACK
                : request.getHeaders().getFirst("User-Agent");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
