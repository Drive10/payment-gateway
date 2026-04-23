package dev.payment.paymentservice.payment.integration.processor;

import dev.payment.paymentservice.payment.config.ServiceConfig;
import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import dev.payment.paymentservice.payment.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.payment.exception.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.math.BigDecimal;
import java.time.Duration;

@Component
@Profile("!test")
public class SimulatorServiceClient implements PaymentProcessorClient {

    private final WebClient webClient;

    public SimulatorServiceClient(ServiceConfig serviceConfig) {
        String baseUrl = serviceConfig.getSimulator().getBaseUrl();
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    @Retry(name = "simulator")
    @CircuitBreaker(name = "simulator", fallbackMethod = "createIntentFallback")
    public PaymentProcessorIntentResponse createIntent(Payment payment, String orderReference, TransactionMode mode) {
        String simulationMode = resolveSimulationMode(mode);
        SimulatorIntentRequest request = new SimulatorIntentRequest(
                orderReference,
                payment.getIdempotencyKey(),
                payment.getProvider(),
                payment.getAmount(),
                payment.getCurrency(),
                simulationMode,
                payment.getNotes()
        );

        return webClient.post()
                .uri("/internal/simulator/payments/intents")
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.value() >= 400,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new ApiException(
                                        HttpStatus.BAD_GATEWAY,
                                        "SIMULATOR_ERROR",
                                        "Simulator error: " + body))))
                .bodyToMono(SimulatorEnvelope.class)
                .map(this::requireEnvelopeData)
                .map(data -> new PaymentProcessorIntentResponse(
                        data.providerOrderId(),
                        data.checkoutUrl(),
                        resolveSimulatedFlag(data)))
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    private String resolveSimulationMode(TransactionMode mode) {
        return mode == TransactionMode.TEST ? "TEST" : "SUCCESS";
    }

    @SuppressWarnings("unused")
    private PaymentProcessorIntentResponse createIntentFallback(Payment payment, String orderReference,
            TransactionMode mode, Throwable throwable) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SIMULATOR_UNAVAILABLE",
                "Simulator service is currently unavailable");
    }

    @Override
    @Retry(name = "simulator")
    @CircuitBreaker(name = "simulator", fallbackMethod = "captureFallback")
    public PaymentProcessorCaptureResponse capture(Payment payment, CapturePaymentRequest request, TransactionMode mode) {
        String simulationMode = resolveSimulationMode(mode);
        SimulatorCaptureRequest captureRequest = new SimulatorCaptureRequest(
                payment.getIdempotencyKey(),
                simulationMode
        );

        return webClient.post()
                .uri("/internal/simulator/payments/{providerOrderId}/capture", payment.getProviderOrderId())
                .bodyValue(captureRequest)
                .retrieve()
                .onStatus(status -> status.value() >= 400,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new ApiException(
                                        HttpStatus.BAD_GATEWAY,
                                        "SIMULATOR_ERROR",
                                        "Simulator capture error: " + body))))
                .bodyToMono(SimulatorEnvelope.class)
                .map(this::requireEnvelopeData)
                .map(data -> new PaymentProcessorCaptureResponse(
                        data.providerPaymentId(),
                        data.providerSignature(),
                        data.providerPaymentId(),
                        resolveSimulatedFlag(data)))
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    @SuppressWarnings("unused")
    private PaymentProcessorCaptureResponse captureFallback(Payment payment, CapturePaymentRequest request,
            TransactionMode mode, Throwable throwable) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SIMULATOR_UNAVAILABLE",
                "Simulator service is currently unavailable for capture");
    }

    private record SimulatorIntentRequest(
            String orderReference,
            String paymentReference,
            String provider,
            BigDecimal amount,
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
            SimulatorData data
    ) {
    }

    private record SimulatorData(
            String providerOrderId,
            String providerPaymentId,
            String providerSignature,
            String checkoutUrl,
            Boolean simulated,
            Boolean testMode
    ) {
    }

    private SimulatorData requireEnvelopeData(SimulatorEnvelope envelope) {
        if (envelope == null || envelope.data() == null) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "SIMULATOR_EMPTY_RESPONSE",
                    "Simulator returned an empty response payload");
        }
        return envelope.data();
    }

    private boolean resolveSimulatedFlag(SimulatorData data) {
        if (data.simulated() != null) {
            return data.simulated();
        }
        return Boolean.TRUE.equals(data.testMode());
    }

    @Override
    public boolean verifyOtp(Payment payment, String otp) {
        if (payment.getTransactionMode() == TransactionMode.TEST) {
            return "123456".equals(otp);
        }
        return otp != null && otp.length() >= 4 && !otp.isBlank();
    }
}
