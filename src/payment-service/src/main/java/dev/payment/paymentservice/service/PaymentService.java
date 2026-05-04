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
import java.math.BigDecimal;
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
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WebhookInboxEventRepository webhookInboxEventRepository;
    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final String OPERATION_IDEMPOTENCY_PREFIX = "operation-idempotency:";
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
            .idempotencyKey(idempotencyKey)
            .clientSecret("pi_" + UUID.randomUUID().toString().replace("-", ""))
            .build();

        payment = paymentRepository.save(payment);

        CreatePaymentResponse response = toPaymentResponse(payment, "Payment intent created");

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

    @Transactional
    public CreatePaymentResponse confirmPaymentIntent(String paymentId, ConfirmPaymentRequest request, String idempotencyKey) {
        CreatePaymentResponse cachedResponse = getOperationCachedResponse(paymentId, "confirm", idempotencyKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.CREATED) {
            throw PaymentException.badRequest("Payment can only be confirmed from CREATED state");
        }

        String method = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "CARD";
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.AUTHORIZATION_PENDING);
        paymentRepository.save(payment);

        if ("CARD".equals(method)) {
            payment.setStatus(PaymentStatus.CHALLENGE_REQUIRED);
            paymentRepository.save(payment);
            saveEvent(paymentId, "PAYMENT_CHALLENGE_REQUIRED", Map.of("paymentId", paymentId));
            CreatePaymentResponse response = toPaymentResponse(payment, "3DS challenge required");
            cacheOperationResponse(paymentId, "confirm", idempotencyKey, response);
            return response;
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);
        paymentRepository.save(payment);
        saveEvent(paymentId, "PAYMENT_AUTHORIZED", Map.of("paymentId", paymentId));
        CreatePaymentResponse response = toPaymentResponse(payment, "Payment authorized");
        cacheOperationResponse(paymentId, "confirm", idempotencyKey, response);
        return response;
    }

    @Transactional
    public CreatePaymentResponse capturePaymentIntent(String paymentId, String idempotencyKey) {
        CreatePaymentResponse cachedResponse = getOperationCachedResponse(paymentId, "capture", idempotencyKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw PaymentException.badRequest("Only authorized payments can be captured");
        }

        if (payment.getCaptureInitiatedAt() != null) {
            throw PaymentException.badRequest("Capture already in progress or completed");
        }

        payment.setCaptureInitiatedAt(Instant.now());
        payment.setCaptureIdempotencyKey(idempotencyKey);
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now());
        paymentRepository.save(payment);
        
        writeCaptureLedgerEntries(payment);
        saveEvent(paymentId, "PAYMENT_CAPTURED", Map.of("paymentId", paymentId, "idempotencyKey", idempotencyKey));
        
        CreatePaymentResponse response = toPaymentResponse(payment, "Payment captured");
        cacheOperationResponse(paymentId, "capture", idempotencyKey, response);
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

    public CreatePaymentResponse getPaymentIntentById(String paymentId) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));
        return toPaymentResponse(payment, null);
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

    public List<PaymentStatusResponse> getAllPayments(int limit, int offset) {
        return paymentRepository.findAll().stream()
            .limit(limit)
            .skip(offset)
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

        if (payment.getStatus() != PaymentStatus.CHALLENGE_REQUIRED && 
            payment.getStatus() != PaymentStatus.AUTHORIZATION_PENDING) {
            throw PaymentException.badRequest("Payment is not in CHALLENGE_REQUIRED or AUTHORIZATION_PENDING state");
        }

        if ("123456".equals(otp)) {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            paymentRepository.save(payment);
            saveEvent(paymentId, "PAYMENT_AUTHORIZED", Map.of("paymentId", paymentId));
            log.info("Payment {} verified successfully", paymentId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid OTP");
            paymentRepository.save(payment);
            throw PaymentException.paymentFailed("Invalid OTP");
        }
    }

    @Transactional
    public void processProviderWebhook(String webhookId, String eventType, String paymentId, String payload) {
        if (webhookInboxEventRepository.existsByWebhookId(webhookId)) {
            log.info("Duplicate webhook received: {}", webhookId);
            return;
        }

        WebhookInboxEvent event = WebhookInboxEvent.builder()
            .webhookId(webhookId)
            .eventType(eventType)
            .paymentId(paymentId)
            .payload(payload)
            .processed(false)
            .build();
        webhookInboxEventRepository.save(event);

        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> PaymentException.notFound("Payment not found: " + paymentId));

        switch (eventType) {
            case "payment.authorized" -> {
                if (payment.getStatus() == PaymentStatus.AUTHORIZATION_PENDING || payment.getStatus() == PaymentStatus.CHALLENGE_REQUIRED) {
                    payment.setStatus(PaymentStatus.AUTHORIZED);
                    paymentRepository.save(payment);
                    saveEvent(paymentId, "PAYMENT_AUTHORIZED", Map.of("paymentId", paymentId, "source", "webhook"));
                }
            }
            case "payment.captured" -> {
                if (payment.getStatus() == PaymentStatus.AUTHORIZED) {
                    payment.setStatus(PaymentStatus.CAPTURED);
                    paymentRepository.save(payment);
                    writeCaptureLedgerEntries(payment);
                    saveEvent(paymentId, "PAYMENT_CAPTURED", Map.of("paymentId", paymentId, "source", "webhook"));
                }
            }
            case "payment.failed" -> {
                if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.REFUNDED) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Provider webhook failure");
                    paymentRepository.save(payment);
                    saveEvent(paymentId, "PAYMENT_FAILED", Map.of("paymentId", paymentId, "source", "webhook"));
                }
            }
            default -> log.warn("Unsupported webhook event type: {}", eventType);
        }

        event.setProcessed(true);
        event.setProcessedAt(Instant.now());
        webhookInboxEventRepository.save(event);
    }

    private void writeCaptureLedgerEntries(Payment payment) {
        String paymentId = payment.getId().toString();
        BigDecimal platformFee = payment.getPlatformFee() != null ? payment.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal gatewayFee = payment.getGatewayFee() != null ? payment.getGatewayFee() : BigDecimal.ZERO;
        BigDecimal merchantNet = payment.getAmount().subtract(platformFee).subtract(gatewayFee);

        persistLedgerEntry(paymentId, null, "CUSTOMER_DEBIT", payment.getAmount(), payment.getCurrency(), paymentId + ":customer_debit");
        if (platformFee.compareTo(BigDecimal.ZERO) > 0) {
            persistLedgerEntry(paymentId, null, "PLATFORM_FEE", platformFee, payment.getCurrency(), paymentId + ":platform_fee");
        }
        if (gatewayFee.compareTo(BigDecimal.ZERO) > 0) {
            persistLedgerEntry(paymentId, null, "GATEWAY_FEE", gatewayFee, payment.getCurrency(), paymentId + ":gateway_fee");
        }
        persistLedgerEntry(paymentId, null, "MERCHANT_CREDIT", merchantNet, payment.getCurrency(), paymentId + ":merchant_credit");
    }

    private void persistLedgerEntry(String paymentId, String refundId, String entryTypeStr, BigDecimal amount, String currency, String reference) {
        if (ledgerEntryRepository.existsByReference(reference)) {
            return;
        }

        LedgerEntry.EntryType entryType = "CUSTOMER_DEBIT".equals(entryTypeStr) || "REFUND_DEBIT_CUSTOMER".equals(entryTypeStr)
            ? LedgerEntry.EntryType.DEBIT : LedgerEntry.EntryType.CREDIT;
        
        LedgerEntry entry = LedgerEntry.builder()
            .paymentId(paymentId)
            .refundId(refundId)
            .entryType(entryType)
            .amount(amount)
            .currency(currency)
            .reference(reference)
            .build();
        ledgerEntryRepository.save(entry);
    }

    private CreatePaymentResponse getOperationCachedResponse(String paymentId, String operation, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String key = OPERATION_IDEMPOTENCY_PREFIX + paymentId + ":" + operation + ":" + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            return null;
        }

        try {
            return objectMapper.readValue(cached, CreatePaymentResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Unable to deserialize operation idempotency cache for key {}", key);
            return null;
        }
    }

    private void cacheOperationResponse(String paymentId, String operation, String idempotencyKey, CreatePaymentResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        String key = OPERATION_IDEMPOTENCY_PREFIX + paymentId + ":" + operation + ":" + idempotencyKey;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(response), IDEMPOTENCY_TTL);
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize operation idempotency response for key {}", key);
        }
    }

    private CreatePaymentResponse toPaymentResponse(Payment payment, String message) {
        boolean requiresAction = payment.getStatus() == PaymentStatus.CHALLENGE_REQUIRED;
        return CreatePaymentResponse.builder()
            .paymentId(payment.getId().toString())
            .transactionId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .checkoutUrl("/pay/" + payment.getId())
            .merchantId(payment.getMerchantId())
            .clientSecret(payment.getClientSecret())
            .requiresAction(requiresAction)
            .nextAction(requiresAction ? "VERIFY_OTP" : null)
            .message(message)
            .build();
    }
}
