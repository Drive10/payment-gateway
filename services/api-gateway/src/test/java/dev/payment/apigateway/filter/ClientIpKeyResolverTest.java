package dev.payment.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpKeyResolverTest {

    private final ClientIpKeyResolver resolver = new ClientIpKeyResolver();

    @Test
    void shouldPreferForwardedForHeader() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/payments")
                .header("X-Forwarded-For", "203.0.113.12, 10.0.0.2")
                .build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("203.0.113.12");
    }
}
