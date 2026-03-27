package dev.payment.apigateway.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements WebFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String resolvedCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID_HEADER, resolvedCorrelationId)
                .build();

        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, resolvedCorrelationId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(context -> context.put(CORRELATION_ID_HEADER, resolvedCorrelationId))
                .doFirst(() -> MDC.put("correlationId", resolvedCorrelationId))
                .doFinally(signalType -> MDC.remove("correlationId"));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
