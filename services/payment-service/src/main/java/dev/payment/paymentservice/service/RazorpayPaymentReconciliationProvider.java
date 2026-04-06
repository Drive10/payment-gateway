package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class RazorpayPaymentReconciliationProvider implements PaymentReconciliationProvider {

    private final RestClient restClient;
    private final boolean enabled;
    private final String authorizationHeader;

    public RazorpayPaymentReconciliationProvider(
            @Value("${application.providers.razorpay.base-url:}") String baseUrl,
            @Value("${application.providers.razorpay.key-id:}") String keyId,
            @Value("${application.providers.razorpay.key-secret:}") String keySecret
    ) {
        this.enabled = baseUrl != null && !baseUrl.isBlank() && keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        if (!enabled) {
            this.restClient = null;
            this.authorizationHeader = null;
            return;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                        .encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8)))
                .build();
        this.authorizationHeader = "configured";
    }

    @Override
    public boolean supports(Payment payment) {
        return enabled
                && authorizationHeader != null
                && payment.getProviderOrderId() != null
                && "RAZORPAY_PRIMARY".equalsIgnoreCase(payment.getProvider());
    }

    @Override
    public Optional<ProviderPaymentSnapshot> lookup(Payment payment) {
        try {
            RazorpayPaymentsEnvelope envelope = restClient.get()
                    .uri("/orders/{providerOrderId}/payments", payment.getProviderOrderId())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new PaymentReconciliationClient.ProviderPaymentMissingException(payment.getProviderOrderId());
                        }
                    })
                    .body(RazorpayPaymentsEnvelope.class);

            if (envelope == null || envelope.items == null || envelope.items.isEmpty()) {
                throw new PaymentReconciliationClient.ProviderPaymentMissingException(payment.getProviderOrderId());
            }

            RazorpayPaymentItem item = envelope.items.get(0);
            return Optional.of(new ProviderPaymentSnapshot(
                    payment.getProviderOrderId(),
                    item.id,
                    item.status,
                    item.amount == null ? payment.getAmount() : item.amount.movePointLeft(2),
                    item.currency == null ? payment.getCurrency() : item.currency,
                    false
            ));
        } catch (RestClientException exception) {
            throw exception;
        }
    }

    private static class RazorpayPaymentsEnvelope {
        public List<RazorpayPaymentItem> items;
    }

    private static class RazorpayPaymentItem {
        public String id;
        public String status;
        public BigDecimal amount;
        public String currency;
    }
}
