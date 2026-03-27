package dev.payment.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenMissing() {
        AtomicReference<String> requestHeader = new AtomicReference<>();
        WebFilterChain chain = exchange -> {
            requestHeader.set(exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/payments").build()
        );

        filter.filter(exchange, chain).block();

        assertThat(requestHeader.get()).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo(requestHeader.get());
    }

    @Test
    void shouldPreserveIncomingCorrelationId() {
        AtomicReference<String> requestHeader = new AtomicReference<>();
        WebFilterChain chain = exchange -> {
            requestHeader.set(exchange.getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER));
            return Mono.empty();
        };

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/payments")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-test-1001")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertThat(requestHeader.get()).isEqualTo("corr-test-1001");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER))
                .isEqualTo("corr-test-1001");
    }
}
