package dev.payment.paymentservice.integration.client;

import dev.payment.paymentservice.config.ServiceConfig;
import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.paymentservice.exception.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("!test")
public class OrderServiceClient {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestClient restClient;

    public OrderServiceClient(ServiceConfig serviceConfig) {
        String baseUrl = serviceConfig.getOrder().getServiceUrl();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderByIdFallback")
    public Order getOrderById(UUID orderId) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/orders/{orderId}", orderId)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found in order-service");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Order order = new Order();
            order.setId(UUID.fromString((String) data.get("id")));
            order.setOrderReference("ORD-" + orderId.toString().substring(0, 8).toUpperCase());
            order.setExternalReference("EXT-" + orderId.toString().substring(0, 8).toUpperCase());
            order.setAmount(new BigDecimal(data.get("amount").toString()));
            order.setCurrency((String) data.get("currency"));
            order.setStatus(mapOrderStatus((String) data.get("status")));
            order.setDescription((String) data.get("description"));
            return order;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Order service call failed: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ORDER_SERVICE_UNAVAILABLE",
                    "Unable to fetch order: " + e.getMessage());
        }
    }

    private OrderStatus mapOrderStatus(String status) {
        if (status == null) {
            return OrderStatus.CREATED;
        }
        return switch (status.toUpperCase()) {
            case "PENDING" -> OrderStatus.CREATED;
            case "PAID" -> OrderStatus.PAID;
            case "FAILED" -> OrderStatus.FAILED;
            case "CANCELLED" -> OrderStatus.FAILED;
            case "EXPIRED" -> OrderStatus.FAILED;
            case "REFUNDED" -> OrderStatus.REFUNDED;
            default -> OrderStatus.CREATED;
        };
    }

    @SuppressWarnings("unused")
    private Order getOrderByIdFallback(UUID orderId, Throwable throwable) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_SERVICE_CIRCUIT_OPEN",
                "Order service is currently unavailable");
    }

    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateOrderStatusFallback")
    public void updateOrderStatus(String orderReference, String status) {
        try {
            restClient.patch()
                    .uri("/orders/{orderReference}/status", orderReference)
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
