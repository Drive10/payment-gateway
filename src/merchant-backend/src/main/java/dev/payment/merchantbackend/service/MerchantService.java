package dev.payment.merchantbackend.service;

import dev.payment.merchantbackend.client.AuthClient;
import dev.payment.merchantbackend.client.PaymentServiceClient;
import dev.payment.merchantbackend.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantService {
    private final PaymentServiceClient paymentClient;
    private final AuthClient authClient;

    public CreatePaymentResponse createPayment(CreatePaymentRequest request, String jwt, String merchantId) {
        log.info("Creating payment for merchant: {}", merchantId);
        String apiKey = authClient.getApiKey(merchantId, jwt);
        if (apiKey == null) {
            log.error("Could not retrieve API key for merchant: {}", merchantId);
            throw new RuntimeException("Could not retrieve API key for merchant");
        }

        log.info("Using API key: {}", apiKey);
        
        String idempotencyKey = "idemp_" + UUID.randomUUID().toString();
        
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("orderId", "ord_" + UUID.randomUUID().toString().substring(0, 8));
        paymentRequest.put("amount", 1000);
        paymentRequest.put("currency", "INR");
        paymentRequest.put("paymentMethod", request.getPaymentMethod());

        log.info("Sending payment request: {}", paymentRequest);
        
        CreatePaymentResponse response = paymentClient.createPayment(apiKey, idempotencyKey, paymentRequest);
        log.info("Payment response: {}", response);
        
        return response;
    }
}
