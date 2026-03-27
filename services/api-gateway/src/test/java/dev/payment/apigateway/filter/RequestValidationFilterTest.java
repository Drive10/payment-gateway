package dev.payment.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationFilterTest {

    private final RequestValidationFilter filter = new RequestValidationFilter(new ObjectMapper());

    @Test
    void shouldRejectMissingIdempotencyKeyForPaymentCreate() {
        MockServerHttpRequest request = MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/payments")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, passthroughChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("400 BAD_REQUEST");
    }

    @Test
    void shouldRejectUnsupportedApiVersion() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v2/payments").build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, passthroughChain()).block();

        assertThat(exchange.getResponse().getStatusCode()).hasToString("404 NOT_FOUND");
    }

    private WebFilterChain passthroughChain() {
        return exchange -> Mono.empty();
    }
}
