package dev.payment.paymentservice.integration.client;

import dev.payment.paymentservice.exception.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!test")
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Value("${application.order.service-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateOrderStatusFallback")
    public void updateOrderStatus(String orderReference, String status) {
        try {
            restClient.patch()
                    .uri("/api/v1/orders/{orderReference}/status", orderReference)
                    .body(new UpdateOrderStatusRequest(status))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ORDER_SERVICE_UNAVAILABLE",
                    "Unable to update order status: " + exception.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void updateOrderStatusFallback(String orderReference, String status, Throwable throwable) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_SERVICE_CIRCUIT_OPEN",
                "Order service is currently unavailable, will retry later");
    }

    private record UpdateOrderStatusRequest(String status) {
    }
}
