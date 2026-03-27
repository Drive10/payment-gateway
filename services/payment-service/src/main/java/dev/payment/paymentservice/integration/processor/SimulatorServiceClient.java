package dev.payment.paymentservice.integration.processor;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!test")
public class SimulatorServiceClient implements PaymentProcessorClient {

    private final RestClient restClient;

    public SimulatorServiceClient(@Value("${application.simulator.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public PaymentProcessorIntentResponse createIntent(Payment payment, String orderReference, TransactionMode mode) {
        try {
            SimulatorResponse response = restClient.post()
                    .uri("/internal/simulator/payments/intents")
                    .body(new SimulatorIntentRequest(
                            orderReference,
                            payment.getIdempotencyKey(),
                            payment.getProvider(),
                            payment.getAmount(),
                            payment.getCurrency(),
                            mode.name(),
                            payment.getNotes()
                    ))
                    .retrieve()
                    .body(SimulatorEnvelope.class)
                    .data();

            return new PaymentProcessorIntentResponse(response.providerOrderId(), response.checkoutUrl(), response.simulated());
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SIMULATOR_UNAVAILABLE", "Unable to create simulator payment intent");
        }
    }

    @Override
    public PaymentProcessorCaptureResponse capture(Payment payment, CapturePaymentRequest request, TransactionMode mode) {
        try {
            SimulatorResponse response = restClient.post()
                    .uri("/internal/simulator/payments/{providerOrderId}/capture", payment.getProviderOrderId())
                    .body(new SimulatorCaptureRequest(payment.getIdempotencyKey(), mode.name()))
                    .retrieve()
                    .body(SimulatorEnvelope.class)
                    .data();

            return new PaymentProcessorCaptureResponse(
                    response.providerPaymentId(),
                    response.providerSignature(),
                    response.providerPaymentId(),
                    response.simulated()
            );
        } catch (RestClientException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "SIMULATOR_UNAVAILABLE", "Unable to capture simulator payment");
        }
    }

    private record SimulatorIntentRequest(
            String orderReference,
            String paymentReference,
            String provider,
            java.math.BigDecimal amount,
            String currency,
            String simulationMode,
            String notes
    ) {
    }

    private record SimulatorCaptureRequest(
            String paymentReference,
            String simulationMode
    ) {
    }

    private record SimulatorEnvelope(
            boolean success,
            SimulatorResponse data
    ) {
    }

    private record SimulatorResponse(
            String providerOrderId,
            String providerPaymentId,
            String providerSignature,
            String checkoutUrl,
            boolean simulated
    ) {
    }
}
