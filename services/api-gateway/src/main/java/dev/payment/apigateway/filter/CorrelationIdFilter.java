package dev.payment.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = extractOrGenerateCorrelationId(exchange);
        
        exchange.getAttributes().put(CORRELATION_ID_KEY, correlationId);
        
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
            return Mono.empty();
        });
        
        return chain.filter(exchange);
    }

    private String extractOrGenerateCorrelationId(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        
        String incomingCorrelationId = headers.getFirst(CORRELATION_ID_HEADER);
        if (incomingCorrelationId != null && !incomingCorrelationId.isBlank()) {
            return incomingCorrelationId;
        }
        
        return generateCorrelationId();
    }

    private String generateCorrelationId() {
        return "corr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toLowerCase();
    }

    private static final String CORRELATION_ID_KEY = "correlationId";
}