package dev.payment.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalGatewayAuthFilter implements GlobalFilter, Ordered {

    public static final String GATEWAY_SECRET_HEADER = "X-Gateway-Token";

    private final String internalSecret;

    public InternalGatewayAuthFilter(@Value("${application.gateway.internal-secret:}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (internalSecret == null || internalSecret.isBlank()) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(GATEWAY_SECRET_HEADER, internalSecret)
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 5;
    }
}
