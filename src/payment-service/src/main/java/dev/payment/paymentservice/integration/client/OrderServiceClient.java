package dev.payment.paymentservice.integration.client;

import dev.payment.paymentservice.config.ServiceConfig;
import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.enums.OrderStatus;
import dev.payment.orderservice.dto.CreateOrderRequest;
import dev.payment.orderservice.dto.OrderResponse;
import dev.payment.paymentservice.exception.ApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    public OrderResponse createOrder(CreateOrderRequest request, String userEmail) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri("/orders")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "ORDER_CREATION_FAILED", 
                        "Failed to create order in order-service");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            return mapToOrderResponse(data);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Order service call failed: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ORDER_SERVICE_UNAVAILABLE",
                    "Unable to create order: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private OrderResponse createOrderFallback(CreateOrderRequest request, String userEmail, Throwable throwable) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_SERVICE_CIRCUIT_OPEN",
                "Order service is currently unavailable");
    }

    @Retry(name = "orderService")
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrdersFallback")
    public Page<OrderResponse> getOrders(UUID userId, OrderStatus status, Pageable pageable, boolean adminView) {
        try {
            // Build query parameters
            StringBuilder uriBuilder = new StringBuilder("/orders");
            boolean firstParam = true;
            
            if (userId != null) {
                uriBuilder.append(firstParam ? "?" : "&").append("userId=").append(userId);
                firstParam = false;
            }
            
            if (status != null) {
                uriBuilder.append(firstParam ? "?" : "&").append("status=").append(status);
                firstParam = false;
            }
            
            if (pageable != null) {
                uriBuilder.append(firstParam ? "?" : "&")
                        .append("page=").append(pageable.getPageNumber())
                        .append("&size=").append(pageable.getPageSize());
            }

            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder.toString())
                    .retrieve()
                    .body(Map.class);

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ORDERS_NOT_FOUND", "Orders not found in order-service");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) response.get("content");
            
            List<OrderResponse> orders = contentList.stream()
                    .map(this::mapToOrderResponse)
                    .toList();

            return new PageImpl<>(
                    orders,
                    PageRequest.of(
                            ((Number) response.getOrDefault("number", 0)).intValue(),
                            ((Number) response.getOrDefault("size", 10)).intValue()
                    ),
                    ((Number) response.getOrDefault("totalElements", 0L)).longValue()
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Order service call failed: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ORDER_SERVICE_UNAVAILABLE",
                    "Unable to fetch orders: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private Page<OrderResponse> getOrdersFallback(UUID userId, OrderStatus status, Pageable pageable, boolean adminView, Throwable throwable) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ORDER_SERVICE_CIRCUIT_OPEN",
                "Order service is currently unavailable");
    }

    private OrderResponse mapToOrderResponse(Map<String, Object> data) {
        dev.payment.orderservice.entity.OrderStatus orderStatus = null;
        if (data.get("status") != null) {
            orderStatus = dev.payment.orderservice.entity.OrderStatus.valueOf((String) data.get("status"));
        }
        return new OrderResponse(
                UUID.fromString((String) data.get("id")),
                data.get("userId") != null ? UUID.fromString((String) data.get("userId")) : null,
                (String) data.get("orderReference"),
                (String) data.get("externalReference"),
                (String) data.get("merchantId"),
                new BigDecimal(data.get("amount").toString()),
                (String) data.get("currency"),
                orderStatus,
                (String) data.get("description"),
                Instant.parse((String) data.get("createdAt")),
                (String) data.get("customerEmail")
        );
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
