package dev.payment.merchantbackend.client;

import dev.payment.merchantbackend.dto.CreatePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {
    private final RestTemplate restTemplate;
    
    @Value("${services.payment-service.url}")
    private String paymentServiceUrl;

    public CreatePaymentResponse createPayment(String apiKey, String idempotencyKey, Map<String, Object> requestBody) {
        log.info("Creating payment with apiKey: {}, idempotencyKey: {}, request: {}", apiKey, idempotencyKey, requestBody);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            headers.set("Idempotency-Key", idempotencyKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<CreatePaymentResponse> response = 
                restTemplate.exchange(paymentServiceUrl + "/api/payments/create-order", HttpMethod.POST, entity, CreatePaymentResponse.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling payment-service: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment: " + e.getMessage());
        }
    }
}
