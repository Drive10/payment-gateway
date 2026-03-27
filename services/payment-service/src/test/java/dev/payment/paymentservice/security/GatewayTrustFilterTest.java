package dev.payment.paymentservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayTrustFilterTest {

    @Test
    void shouldRejectNonGatewayTrafficForProtectedEndpoints() throws Exception {
        GatewayTrustFilter filter = new GatewayTrustFilter(new ObjectMapper(), true, "trusted-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("UNTRUSTED_GATEWAY");
    }

    @Test
    void shouldAllowTrustedGatewayTraffic() throws Exception {
        GatewayTrustFilter filter = new GatewayTrustFilter(new ObjectMapper(), true, "trusted-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments");
        request.addHeader(GatewayTrustFilter.GATEWAY_SECRET_HEADER, "trusted-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void shouldBypassGatewayCheckForWebhookEndpoint() throws Exception {
        GatewayTrustFilter filter = new GatewayTrustFilter(new ObjectMapper(), true, "trusted-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/webhooks/razorpay");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
