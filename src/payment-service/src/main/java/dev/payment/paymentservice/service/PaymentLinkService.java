package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.exception.PaymentException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class PaymentLinkService {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PAYMENT_LINK_PREFIX = "payment_link:";
    private static final long DEFAULT_EXPIRY_HOURS = 24;
    private static final String CHECKOUT_BASE_URL = System.getenv("CHECKOUT_BASE_URL") != null 
        ? System.getenv("CHECKOUT_BASE_URL") 
        : "http://localhost:5173";

    private static final Map<String, String> SEEDED_MERCHANTS = new HashMap<>();
    static {
        SEEDED_MERCHANTS.put("33333333-3333-3333-3333-333333333333", "Demo Electronics Store");
        SEEDED_MERCHANTS.put("44444444-4444-4444-4444-444444444444", "Demo Fashion Boutique");
    }

    public PaymentLinkService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public PaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request, String merchantId) {
        String referenceId = generateReferenceId();
        
        String merchantName = SEEDED_MERCHANTS.getOrDefault(merchantId, "Demo Store");
        
        long expiryHours = request.getExpiresInHours() != null ? request.getExpiresInHours() : DEFAULT_EXPIRY_HOURS;
        Instant expiresAt = Instant.now().plusSeconds(expiryHours * 3600);

        PaymentLinkData linkData = PaymentLinkData.builder()
                .referenceId(referenceId)
                .merchantId(merchantId)
                .merchantName(merchantName)
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency().toUpperCase() : "INR")
                .description(request.getDescription())
                .customerEmail(request.getCustomerEmail())
                .customerMobile(request.getCustomerMobile())
                .status("PENDING")
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();

        try {
            String json = objectMapper.writeValueAsString(linkData);
            redisTemplate.opsForValue().set(
                    PAYMENT_LINK_PREFIX + referenceId,
                    json,
                    Duration.ofHours(expiryHours)
            );
            log.info("Payment link created: {} for merchant: {}", referenceId, merchantId);
        } catch (Exception e) {
            log.error("Error storing payment link: {}", e.getMessage());
            throw PaymentException.internal("Failed to create payment link");
        }

        return PaymentLinkResponse.builder()
                .referenceId(referenceId)
                .checkoutUrl(CHECKOUT_BASE_URL + "/checkout?ref=" + referenceId)
                .amount(linkData.getAmount().toString())
                .currency(linkData.getCurrency())
                .merchantName(merchantName)
                .description(linkData.getDescription())
                .status(linkData.getStatus())
                .expiresAt(expiresAt)
                .createdAt(linkData.getCreatedAt())
                .build();
    }

    public PaymentLinkData getPaymentLink(String referenceId) {
        String key = PAYMENT_LINK_PREFIX + referenceId;
        String json = redisTemplate.opsForValue().get(key);
        
        if (json == null) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, PaymentLinkData.class);
        } catch (Exception e) {
            log.error("Error reading payment link: {}", e.getMessage());
            return null;
        }
    }

    private String generateReferenceId() {
        return "PL" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentLinkData {
        private String referenceId;
        private String merchantId;
        private String merchantName;
        private java.math.BigDecimal amount;
        private String currency;
        private String description;
        private String customerEmail;
        private String customerMobile;
        private String status;
        private Instant expiresAt;
        private Instant createdAt;
    }
}