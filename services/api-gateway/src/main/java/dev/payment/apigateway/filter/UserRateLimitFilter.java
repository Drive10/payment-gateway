package dev.payment.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class UserRateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;

    private static final int USER_RATE_LIMIT = 100;
    private static final int USER_RATE_LIMIT_WINDOW = 60;

    public UserRateLimitFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = extractUserId(exchange);
        
        if (userId == null) {
            return chain.filter(exchange);
        }

        String rateLimitKey = "rate_limit:user:" + userId;

        return redisTemplate.opsForValue()
                .increment(rateLimitKey)
                .flatMap(currentCount -> {
                    if (currentCount == 1) {
                        return redisTemplate.expire(rateLimitKey, Duration.ofSeconds(USER_RATE_LIMIT_WINDOW))
                                .thenReturn(currentCount);
                    }
                    return Mono.just(currentCount);
                })
                .flatMap(count -> {
                    if (count > USER_RATE_LIMIT) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(USER_RATE_LIMIT));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(USER_RATE_LIMIT - count));
                    return chain.filter(exchange);
                })
                .onErrorResume(e -> chain.filter(exchange));
    }

    private String extractUserId(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return "user_" + token.hashCode();
        }
        
        String xUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (xUserId != null) {
            return "user_" + xUserId;
        }

        String clientIp = exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
        return "ip_" + clientIp.hashCode();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}