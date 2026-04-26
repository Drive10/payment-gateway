package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.*;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.exception.ErrorCodes;
import dev.payment.paymentservice.exception.PaymentException;
import dev.payment.paymentservice.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OutboxRepository outboxRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);

    @Transactional
    public CreatePaymentResponse createPayment(String idempotencyKey, CreateOrderRequest request, String merchantId) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            String cached = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
            if (cached != null) {
                log.info("Duplicate request detected for idempotency key: {}. Returning cached response.", idempotencyKey);
                try {
                    return objectMapper.readValue(cached, CreatePaymentResponse.class);
                } catch (JsonProcessingException e) {
                    log.error("Error deserializing cached idempotency response: {}", e.getMessage());
                }
            }
        }

        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .amount(request.getAmount())
            .currency(request.getCurrency().toUpperCase())
            .status(PaymentStatus.CREATED)
            .merchantId(merchantId)
            .paymentMethod(request.getPaymentMethod())
            .correlationId(UUID.randomUUID().toString())
            .build();

        payment = paymentRepository.save(payment);

        CreatePaymentResponse response = CreatePaymentResponse.builder()
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .checkoutUrl("/pay/" + payment.getId())
            .merchantId(payment.getMerchantId())
            .build();

        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            try {
                String jsonResponse = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(
                    IDEMPOTENCY_PREFIX + idempotencyKey,
                    jsonResponse,
                    IDEMPOTENCY_TTL
                );
            } catch (JsonProcessingException e) {
                log.error("Error serializing response for idempotency: {}", e.getMessage());
            }
        }

        saveEvent(payment.getId().toString(), "PAYMENT_CREATED", response);
        log.info("Payment created: {} for order: {}", payment.getId(), payment.getOrderId());

        return response;
    }

    private void saveEvent(String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            Outbox event = Outbox.builder()
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(jsonPayload)
                .createdAt(Instant.now())
                .build();
            outboxRepository.save(event);
        } catch (JsonProcessingException e) {
            log.error("Error serializing outbox payload: {}", e.getMessage());
        }
    }

    public PaymentStatusResponse getPaymentStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + orderId));

        return PaymentStatusResponse.builder()
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .failureReason(payment.getFailureReason())
            .build();
    }

public PaymentStatusResponse getPaymentStatusById(String paymentId) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));

        return PaymentStatusResponse.builder()
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .failureReason(payment.getFailureReason())
            .build();
    }

    public List<PaymentStatusResponse> getMerchantOrders(String merchantId) {
        return paymentRepository.findByMerchantId(merchantId).stream()
            .map(p -> PaymentStatusResponse.builder()
                .paymentId(p.getId().toString())
                .orderId(p.getOrderId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus().name())
                .failureReason(p.getFailureReason())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional
    public void updatePaymentStatus(String paymentId, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));

        if (!payment.canTransitionTo(newStatus)) {
            throw PaymentException.badRequest(
                "Invalid state transition from " + payment.getStatus() + " to " + newStatus
            );
        }

        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        
        saveEvent(paymentId, "PAYMENT_STATUS_UPDATED", Map.of(
            "paymentId", paymentId,
            "oldStatus", payment.getStatus(),
            "newStatus", newStatus
        ));
        
        log.info("Payment {} transitioned to {}", paymentId, newStatus);
    }

    @Transactional
    public void verifyOtp(String paymentId, String otp) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CHALLENGE_REQUIRED) {
            throw PaymentException.badRequest("Payment is not in CHALLENGE_REQUIRED state");
        }

        if ("123456".equals(otp)) {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            paymentRepository.save(payment);
            log.info("Payment {} verified successfully", paymentId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid OTP");
            paymentRepository.save(payment);
            throw PaymentException.paymentFailed("Invalid OTP");
        }
    }
}