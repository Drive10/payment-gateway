package dev.payment.graphql.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentGraphQLController {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.payment-service-url:http://localhost:8082}")
    private String paymentServiceUrl;

    @Value("${services.order-service-url:http://localhost:8083}")
    private String orderServiceUrl;

    @Value("${services.analytics-service-url:http://localhost:8086}")
    private String analyticsServiceUrl;

    @Value("${services.simulator-service-url:http://localhost:8085}")
    private String simulatorServiceUrl;

    @Value("${services.search-service-url:http://localhost:8088}")
    private String searchServiceUrl;

    private WebClient getWebClient(String baseUrl) {
        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @QueryMapping
    public Map<String, Object> searchPayments(
            @Argument String query,
            @Argument Integer page,
            @Argument Integer pageSize) {
        
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        log.info("[GraphQL] SearchPayments: query={}, page={}, size={}", query, p, size);
        
        try {
            Map<String, Object> response = getWebClient(searchServiceUrl)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/search/payments")
                            .queryParam("q", query)
                            .queryParam("page", p)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Search service unavailable, returning empty result: {}", e.getMessage());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("payments", new ArrayList<>());
        result.put("totalCount", 0);
        result.put("page", p);
        result.put("pageSize", size);
        result.put("hasMore", false);
        return result;
    }

    @QueryMapping
    public Map<String, Object> analyticsSummary(
            @Argument String startDate,
            @Argument String endDate) {
        
        log.info("[GraphQL] AnalyticsSummary: startDate={}, endDate={}", startDate, endDate);
        
        try {
            String uri = "/api/v1/analytics/summary";
            if (startDate != null && endDate != null) {
                uri += "?startDate=" + startDate + "&endDate=" + endDate;
            }
            
            Map<String, Object> response = getWebClient(analyticsServiceUrl)
                    .get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Analytics service unavailable: {}", e.getMessage());
        }
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTransactions", 0);
        summary.put("successfulTransactions", 0);
        summary.put("failedTransactions", 0);
        summary.put("totalVolume", 0.0);
        summary.put("averageTransactionValue", 0.0);
        summary.put("successRate", 0.0);
        return summary;
    }

    @QueryMapping
    public List<Map<String, Object>> dailyMetrics(@Argument Integer days) {
        int d = days != null ? days : 7;
        log.info("[GraphQL] DailyMetrics: days={}", d);
        
        try {
            List<Map> response = getWebClient(analyticsServiceUrl)
                    .get()
                    .uri("/api/v1/analytics/daily?days=" + d)
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return new ArrayList<>(response);
            }
        } catch (Exception e) {
            log.warn("Analytics service unavailable: {}", e.getMessage());
        }
        
        return new ArrayList<>();
    }

    @QueryMapping
    public List<Map<String, Object>> simulatorConfigs() {
        log.info("[GraphQL] SimulatorConfigs");
        
        try {
            List<Map> response = getWebClient(simulatorServiceUrl)
                    .get()
                    .uri("/api/v1/simulator/configs")
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return new ArrayList<>(response);
            }
        } catch (Exception e) {
            log.warn("Simulator service unavailable: {}", e.getMessage());
        }
        
        return getMockSimulatorConfigs();
    }

    @QueryMapping
    public Map<String, Object> payments(
            @Argument Map<String, Object> filter,
            @Argument Integer page,
            @Argument Integer pageSize) {
        
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        log.info("[GraphQL] Payments: filter={}, page={}, size={}", filter, p, size);
        
        try {
            StringBuilder uri = new StringBuilder("/api/v1/payments?page=" + p + "&size=" + size);
            if (filter != null) {
                if (filter.containsKey("status")) {
                    uri.append("&status=").append(filter.get("status"));
                }
                if (filter.containsKey("paymentMethod")) {
                    uri.append("&paymentMethod=").append(filter.get("paymentMethod"));
                }
            }
            
            Map<String, Object> response = getWebClient(paymentServiceUrl)
                    .get()
                    .uri(uri.toString())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Payment service unavailable: {}", e.getMessage());
        }
        
        return getMockPaymentsPage(p, size);
    }

    @QueryMapping
    public Map<String, Object> payment(@Argument String id) {
        log.info("[GraphQL] Payment: id={}", id);
        
        try {
            Map<String, Object> response = getWebClient(paymentServiceUrl)
                    .get()
                    .uri("/api/v1/payments/" + id)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Payment service unavailable: {}", e.getMessage());
        }
        
        return createSamplePayment(1);
    }

    @QueryMapping
    public List<Map<String, Object>> orders(
            @Argument Integer page,
            @Argument Integer pageSize) {
        
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        log.info("[GraphQL] Orders: page={}, size={}", p, size);
        
        try {
            List<Map> response = getWebClient(orderServiceUrl)
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/orders")
                            .queryParam("page", p)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .bodyToMono(List.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return new ArrayList<>(response);
            }
        } catch (Exception e) {
            log.warn("Order service unavailable: {}", e.getMessage());
        }
        
        return getMockOrders(size);
    }

    @QueryMapping
    public Map<String, Object> order(@Argument String id) {
        log.info("[GraphQL] Order: id={}", id);
        
        try {
            Map<String, Object> response = getWebClient(orderServiceUrl)
                    .get()
                    .uri("/api/v1/orders/" + id)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.warn("Order service unavailable: {}", e.getMessage());
        }
        
        return createSampleOrder(1);
    }

    @MutationMapping
    public Map<String, Object> createPayment(@Argument Map<String, Object> input) {
        log.info("[GraphQL] CreatePayment: input={}", input);
        
        try {
            Map<String, Object> response = getWebClient(paymentServiceUrl)
                    .post()
                    .uri("/api/v1/payments")
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to create payment: {}", e.getMessage());
        }
        
        Map<String, Object> payment = new HashMap<>();
        payment.put("id", UUID.randomUUID().toString());
        payment.put("orderId", input.get("orderId"));
        payment.put("amount", input.get("amount"));
        payment.put("currency", input.getOrDefault("currency", "INR"));
        payment.put("paymentMethod", input.get("paymentMethod"));
        payment.put("status", "PENDING");
        payment.put("transactionMode", "TEST");
        payment.put("simulated", true);
        payment.put("createdAt", new Date().toString());
        return payment;
    }

    @MutationMapping
    public Map<String, Object> refundPayment(
            @Argument String paymentId,
            @Argument Float amount,
            @Argument String reason) {
        
        log.info("[GraphQL] RefundPayment: paymentId={}, amount={}", paymentId, amount);
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amount);
            requestBody.put("reason", reason);
            
            Map<String, Object> response = getWebClient(paymentServiceUrl)
                    .post()
                    .uri("/api/v1/payments/" + paymentId + "/refund")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to refund payment: {}", e.getMessage());
        }
        
        Map<String, Object> refund = new HashMap<>();
        refund.put("id", UUID.randomUUID().toString());
        refund.put("paymentId", paymentId);
        refund.put("amount", amount != null ? amount : 0.0);
        refund.put("status", "PENDING");
        refund.put("reason", reason);
        refund.put("createdAt", new Date().toString());
        return refund;
    }

    @MutationMapping
    public Map<String, Object> createOrder(@Argument Map<String, Object> input) {
        log.info("[GraphQL] CreateOrder: input={}", input);
        
        try {
            Map<String, Object> response = getWebClient(orderServiceUrl)
                    .post()
                    .uri("/api/v1/orders")
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to create order: {}", e.getMessage());
        }
        
        return createSampleOrder(1);
    }

    @MutationMapping
    public Map<String, Object> cancelOrder(@Argument String orderId) {
        log.info("[GraphQL] CancelOrder: orderId={}", orderId);
        
        try {
            Map<String, Object> response = getWebClient(orderServiceUrl)
                    .post()
                    .uri("/api/v1/orders/" + orderId + "/cancel")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to cancel order: {}", e.getMessage());
        }
        
        Map<String, Object> order = createSampleOrder(1);
        order.put("id", orderId);
        order.put("status", "CANCELLED");
        return order;
    }

    @MutationMapping
    public Map<String, Object> updateSimulatorConfig(
            @Argument String providerName,
            @Argument Float successRate,
            @Argument Integer latencyMs) {
        
        log.info("[GraphQL] UpdateSimulatorConfig: {}", providerName);
        
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("providerName", providerName);
            requestBody.put("successRate", successRate);
            requestBody.put("averageLatencyMs", latencyMs);
            requestBody.put("isActive", true);
            
            Map<String, Object> response = getWebClient(simulatorServiceUrl)
                    .put()
                    .uri("/api/v1/simulator/configs")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to update simulator config: {}", e.getMessage());
        }
        
        Map<String, Object> config = new HashMap<>();
        config.put("id", UUID.randomUUID().toString());
        config.put("providerName", providerName);
        config.put("successRate", successRate);
        config.put("averageLatencyMs", latencyMs);
        config.put("isActive", true);
        return config;
    }

    private List<Map<String, Object>> getMockSimulatorConfigs() {
        List<Map<String, Object>> configs = new ArrayList<>();
        
        Map<String, Object> razorpay = new HashMap<>();
        razorpay.put("id", UUID.randomUUID().toString());
        razorpay.put("providerName", "RAZORPAY");
        razorpay.put("successRate", 0.90);
        razorpay.put("averageLatencyMs", 1500);
        razorpay.put("isActive", true);
        configs.add(razorpay);
        
        Map<String, Object> stripe = new HashMap<>();
        stripe.put("id", UUID.randomUUID().toString());
        stripe.put("providerName", "STRIPE");
        stripe.put("successRate", 0.92);
        stripe.put("averageLatencyMs", 1200);
        stripe.put("isActive", true);
        configs.add(stripe);
        
        return configs;
    }

    private Map<String, Object> getMockPaymentsPage(int page, int size) {
        List<Map<String, Object>> payments = new ArrayList<>();
        for (int i = 0; i < Math.min(5, size); i++) {
            payments.add(createSamplePayment(i));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("payments", payments);
        result.put("totalCount", 5);
        result.put("page", page);
        result.put("pageSize", size);
        result.put("hasMore", page < 1);
        return result;
    }

    private List<Map<String, Object>> getMockOrders(int size) {
        List<Map<String, Object>> orders = new ArrayList<>();
        for (int i = 0; i < Math.min(5, size); i++) {
            orders.add(createSampleOrder(i));
        }
        return orders;
    }

    private Map<String, Object> createSamplePayment(int index) {
        Map<String, Object> payment = new HashMap<>();
        payment.put("id", UUID.randomUUID().toString());
        payment.put("orderId", UUID.randomUUID().toString());
        payment.put("amount", 1000.00 + (index * 500));
        payment.put("currency", "INR");
        payment.put("paymentMethod", index % 2 == 0 ? "CARD" : "UPI");
        payment.put("provider", "RAZORPAY");
        payment.put("providerPaymentId", "pay_" + UUID.randomUUID().toString().substring(0, 8));
        payment.put("status", index % 3 == 0 ? "COMPLETED" : "PENDING");
        payment.put("transactionMode", "TEST");
        payment.put("simulated", true);
        payment.put("createdAt", new Date().toString());
        return payment;
    }

    private Map<String, Object> createSampleOrder(int index) {
        Map<String, Object> order = new HashMap<>();
        order.put("id", UUID.randomUUID().toString());
        order.put("externalReference", "ORD-2026-" + String.format("%04d", index + 1));
        order.put("amount", 1500.00 + (index * 1000));
        order.put("currency", "INR");
        order.put("description", "Sample Order #" + (index + 1));
        order.put("status", index % 2 == 0 ? "COMPLETED" : "PENDING");
        order.put("merchantId", "merchant_001");
        order.put("createdAt", new Date().toString());
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("id", UUID.randomUUID().toString());
        item.put("productName", "Product " + (index + 1));
        item.put("quantity", index + 1);
        item.put("unitPrice", 500.00);
        item.put("totalPrice", (index + 1) * 500.00);
        items.add(item);
        order.put("items", items);
        
        return order;
    }
}
