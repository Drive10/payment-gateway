package dev.payment.apigateway.filter;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Component("clientIpKeyResolver")
public class ClientIpKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(org.springframework.web.server.ServerWebExchange exchange) {
        return Mono.just(resolveClientKey(exchange.getRequest()));
    }

    private String resolveClientKey(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            String hostAddress = remoteAddress.getAddress().getHostAddress();
            if (hostAddress != null) {
                return hostAddress;
            }
        }
        
        String userAgent = request.getHeaders().getFirst("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? "anonymous" : userAgent;
    }
}
