package dev.payment.paymentservice.service;

import dev.payment.common.api.ApiResponse;
import dev.payment.paymentservice.domain.Payment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentReconciliationClient {

    private static final ParameterizedTypeReference<ApiResponse<SimulationLookupResponse>> SIMULATION_RESPONSE =
            new ParameterizedTypeReference<>() {};

    private final RestClient simulatorClient;
    private final boolean simulatorEnabled;

    public PaymentReconciliationClient(@Value("${application.simulator.base-url:}") String simulatorBaseUrl) {
        this.simulatorEnabled = simulatorBaseUrl != null && !simulatorBaseUrl.isBlank();
        if (!simulatorEnabled) {
            this.simulatorClient = null;
            return;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(5000);

        this.simulatorClient = RestClient.builder()
                .baseUrl(simulatorBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Retry(name = "simulator")
    @CircuitBreaker(name = "simulator")
    public Optional<ProviderPaymentSnapshot> lookup(Payment payment) {
        if (!supports(payment)) {
            return Optional.empty();
        }

        try {
            ApiResponse<SimulationLookupResponse> apiResponse = simulatorClient.get()
                    .uri("/internal/simulator/payments/{providerOrderId}", payment.getProviderOrderId())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        if (response.getStatusCode().value() == 404) {
                            throw new ProviderPaymentMissingException(payment.getProviderOrderId());
                        }
                    })
                    .body(SIMULATION_RESPONSE);

            if (apiResponse == null || apiResponse.data() == null) {
                return Optional.empty();
            }

            SimulationLookupResponse body = apiResponse.data();
            return Optional.of(new ProviderPaymentSnapshot(
                    body.providerOrderId(),
                    body.providerPaymentId(),
                    body.status(),
                    body.amount(),
                    body.currency(),
                    body.simulated()
            ));
        } catch (RestClientException exception) {
            throw exception;
        }
    }

    private boolean supports(Payment payment) {
        return simulatorEnabled && payment.getProviderOrderId() != null
                && (payment.isSimulated() || "RAZORPAY_SIMULATOR".equalsIgnoreCase(payment.getProvider()));
    }

    private record SimulationLookupResponse(
            String providerOrderId,
            String providerPaymentId,
            String status,
            BigDecimal amount,
            String currency,
            boolean simulated
    ) {
    }

    public static class ProviderPaymentMissingException extends RuntimeException {
        public ProviderPaymentMissingException(String providerOrderId) {
            super("Provider payment not found for order " + providerOrderId);
        }
    }
}
