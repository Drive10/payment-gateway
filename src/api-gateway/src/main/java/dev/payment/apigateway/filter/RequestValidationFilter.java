package dev.payment.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.api.ErrorDetails;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
public class RequestValidationFilter implements WebFilter, Ordered {

    private static final Set<HttpMethod> BODY_METHODS = Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private final ObjectMapper objectMapper;

    public RequestValidationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (path.startsWith("/api/") && !path.startsWith("/api/v1/")) {
            return writeError(exchange, HttpStatus.NOT_FOUND, "UNSUPPORTED_API_VERSION", "Only /api/v1 routes are currently supported");
        }

        if (method != null && BODY_METHODS.contains(method)
                && !path.startsWith("/api/v1/webhooks/")) {
            MediaType contentType = exchange.getRequest().getHeaders().getContentType();
            if (contentType == null || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return writeError(exchange, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_CONTENT_TYPE", "application/json content type is required");
            }
        }

        if (HttpMethod.POST.equals(method) && (path.equals("/api/v1/payments") || path.matches("^/api/v1/payments/[^/]+/refunds$"))) {
            String idempotencyKey = exchange.getRequest().getHeaders().getFirst("Idempotency-Key");
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                return writeError(exchange, HttpStatus.BAD_REQUEST, "MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required");
            }
        }

        return chain.filter(exchange);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(ApiResponse.failure(new ErrorDetails(code, message, null)));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
        } catch (Exception exception) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
