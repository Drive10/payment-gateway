package dev.payment.merchantbackend.service;

import dev.payment.common.dto.OrderResponse;
import dev.payment.common.dto.PaymentResponse;
import dev.payment.merchantbackend.dto.CreatePaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private final WebClient webClient;
    private final String paymentServiceUrl;
    private final String apiKey;

    public PaymentService(
            WebClient webClient,
            @Value("${payment-service.url}") String paymentServiceUrl,
            @Value("${payment-service.api-key:sk_test_merchant123}") String apiKey) {
        this.webClient = webClient;
        this.paymentServiceUrl = paymentServiceUrl;
        this.apiKey = apiKey;
    }

    public CreatePaymentResponse createPayment(String productId, String customerEmail) {
        UUID orderId = UUID.randomUUID();
        
        OrderResponse order = createOrderSnapshot(orderId, productId, customerEmail);
        
        PaymentResponse payment = callPaymentService(order);
        
        return CreatePaymentResponse.from(order, payment);
    }

    private OrderResponse createOrderSnapshot(UUID orderId, String productId, String customerEmail) {
        OrderResponse order = new OrderResponse();
        order.setId(orderId);
        order.setOrderReference("order_" + orderId.toString().substring(0, 8));
        order.setExternalReference("ext_" + System.currentTimeMillis());
        order.setAmount(BigDecimal.valueOf(50000));
        order.setCurrency("INR");
        order.setStatus("PENDING");
        order.setDescription("Payment for product: " + productId);
        order.setCreatedAt(Instant.now());
        order.setCustomerEmail(customerEmail);
        return order;
    }

    private PaymentResponse callPaymentService(OrderResponse order) {
        Map<String, Object> requestBody = Map.of(
            "order", Map.of(
                "id", order.getId().toString(),
                "amount", order.getAmount(),
                "currency", order.getCurrency()
            ),
            "method", "CARD"
        );

        String idempotencyKey = "idem_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(paymentServiceUrl + "/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return mapToPaymentResponse(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }

        return null;
    }

    private PaymentResponse mapToPaymentResponse(Map<String, Object> data) {
        PaymentResponse payment = new PaymentResponse();
        payment.setId(UUID.fromString((String) data.get("id")));
        
        if (data.get("orderId") != null) {
            payment.setOrderId(UUID.fromString(String.valueOf(data.get("orderId"))));
        }
        
        payment.setOrderReference((String) data.get("orderReference"));
        payment.setAmount(new BigDecimal(String.valueOf(data.get("amount"))));
        
        if (data.get("refundedAmount") != null) {
            payment.setRefundedAmount(new BigDecimal(String.valueOf(data.get("refundedAmount"))));
        } else {
            payment.setRefundedAmount(BigDecimal.ZERO);
        }
        
        payment.setCurrency((String) data.get("currency"));
        payment.setProvider((String) data.get("provider"));
        payment.setProviderOrderId((String) data.get("providerOrderId"));
        payment.setProviderPaymentId((String) data.get("providerPaymentId"));
        payment.setMethod((String) data.get("method"));
        payment.setTransactionMode((String) data.get("transactionMode"));
        payment.setStatus((String) data.get("status"));
        payment.setCheckoutUrl((String) data.get("checkoutUrl"));
        payment.setSimulated(data.get("simulated") != null ? (Boolean) data.get("simulated") : false);
        payment.setProviderSignature((String) data.get("providerSignature"));
        payment.setNotes((String) data.get("notes"));
        
        if (data.get("createdAt") != null) {
            payment.setCreatedAt(Instant.parse(String.valueOf(data.get("createdAt"))));
        } else {
            payment.setCreatedAt(Instant.now());
        }
        
        return payment;
    }
}