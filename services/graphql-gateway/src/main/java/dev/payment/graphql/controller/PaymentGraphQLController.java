package dev.payment.graphql.controller;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import java.util.*;

@Controller
public class PaymentGraphQLController {

    // ============ QUERIES ============

    @QueryMapping
    public Map<String, Object> searchPayments(
            @Argument String query,
            @Argument Integer page,
            @Argument Integer pageSize) {
        
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        System.out.println("[GraphQL] SearchPayments: query=" + query + ", page=" + p + ", size=" + size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("payments", new ArrayList<>());
        result.put("totalCount", 0);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("hasMore", false);
        
        return result;
    }

    @QueryMapping
    public Map<String, Object> analyticsSummary(
            @Argument String startDate,
            @Argument String endDate) {
        
        System.out.println("[GraphQL] AnalyticsSummary: startDate=" + startDate + ", endDate=" + endDate);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTransactions", 1250);
        summary.put("successfulTransactions", 1187);
        summary.put("failedTransactions", 63);
        summary.put("totalVolume", 2567500.00);
        summary.put("averageTransactionValue", 2054.00);
        summary.put("successRate", 94.96);
        
        return summary;
    }

    @QueryMapping
    public List<Map<String, Object>> dailyMetrics(@Argument Integer days) {
        int d = days != null ? days : 7;
        System.out.println("[GraphQL] DailyMetrics: days=" + days);
        
        List<Map<String, Object>> metrics = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", "2026-04-0" + (5 - i));
            day.put("newUsers", 5 + i);
            day.put("totalUsers", 100 + i * 5);
            day.put("newMerchants", 2);
            day.put("totalMerchants", 20 + i);
            day.put("totalTransactions", 150 + i * 10);
            day.put("successfulTransactions", 142 + i * 9);
            day.put("failedTransactions", 8 + i);
            day.put("totalVolume", 250000.00 + i * 10000);
            metrics.add(day);
        }
        
        return metrics;
    }

    @QueryMapping
    public List<Map<String, Object>> simulatorConfigs() {
        System.out.println("[GraphQL] SimulatorConfigs");
        
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

    @QueryMapping
    public List<Map<String, Object>> payments(
            @Argument Map<String, Object> filter,
            @Argument Integer page,
            @Argument Integer pageSize) {
        
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        System.out.println("[GraphQL] Payments: filter=" + filter + ", page=" + p + ", size=" + size);
        
        List<Map<String, Object>> payments = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> payment = createSamplePayment(i);
            payments.add(payment);
        }
        
        return payments;
    }

    @QueryMapping
    public Map<String, Object> payment(@Argument String id) {
        System.out.println("[GraphQL] Payment: id=" + id);
        return createSamplePayment(1);
    }

    @QueryMapping
    public List<Map<String, Object>> orders(
            @Argument Integer page,
            @Argument Integer pageSize) {
        
        int p = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 20;
        System.out.println("[GraphQL] Orders: page=" + p + ", size=" + size);
        
        List<Map<String, Object>> orders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> order = createSampleOrder(i);
            orders.add(order);
        }
        
        return orders;
    }

    @QueryMapping
    public Map<String, Object> order(@Argument String id) {
        System.out.println("[GraphQL] Order: id=" + id);
        return createSampleOrder(1);
    }

    // ============ MUTATIONS ============

    @MutationMapping
    public Map<String, Object> createPayment(@Argument Map<String, Object> input) {
        System.out.println("[GraphQL] CreatePayment: input=" + input);
        
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
        
        System.out.println("[GraphQL] RefundPayment: paymentId=" + paymentId + ", amount=" + amount);
        
        Map<String, Object> refund = new HashMap<>();
        refund.put("id", UUID.randomUUID().toString());
        refund.put("orderId", paymentId);
        refund.put("amount", amount != null ? amount : 0.0);
        refund.put("status", "PENDING");
        refund.put("reason", reason);
        refund.put("createdAt", new Date().toString());
        
        return refund;
    }

    @MutationMapping
    public Map<String, Object> createOrder(@Argument Map<String, Object> input) {
        System.out.println("[GraphQL] CreateOrder: input=" + input);
        
        Map<String, Object> order = createSampleOrder(1);
        order.put("amount", input.get("amount"));
        order.put("description", input.get("description"));
        
        return order;
    }

    @MutationMapping
    public Map<String, Object> cancelOrder(@Argument String orderId) {
        System.out.println("[GraphQL] CancelOrder: orderId=" + orderId);
        
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
        
        System.out.println("[GraphQL] UpdateSimulatorConfig: " + providerName);
        
        Map<String, Object> config = new HashMap<>();
        config.put("id", UUID.randomUUID().toString());
        config.put("providerName", providerName);
        config.put("successRate", successRate);
        config.put("averageLatencyMs", latencyMs);
        config.put("isActive", true);
        
        return config;
    }

    // ============ HELPER METHODS ============

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
