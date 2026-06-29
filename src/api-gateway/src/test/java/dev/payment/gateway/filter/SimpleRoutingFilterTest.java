package dev.payment.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimpleRoutingFilterTest {

    private GatewayFilterChain filterChain;
    private SimpleRoutingFilter filter;

    @BeforeEach
    void setUp() {
        filterChain = mock(GatewayFilterChain.class);
        filter = new SimpleRoutingFilter();
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void filter_ShouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/any/path").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }

    @Test
    void filter_WithPostRequest_ShouldPassThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/payments/create")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
    }
}
