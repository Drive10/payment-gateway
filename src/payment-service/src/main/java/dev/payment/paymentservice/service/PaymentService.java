package dev.payment.paymentservice.service;

import dev.payment.paymentservice.dto.*;
import dev.payment.paymentservice.entity.Payment;
import dev.payment.paymentservice.entity.Payment.PaymentStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofMinutes(30);

    @Transactional
    public CreatePaymentResponse createPayment(String idempotencyKey, CreateOrderRequest request, String merchantId) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            String cached = redisTemplate.opsForValue().get(IDEMPOTENCY_PREFIX + idempotencyKey);
            if (cached != null) {
                log.info("Duplicate request detected for idempotency key: {}", idempotencyKey);
                throw new RuntimeException("Duplicate request. Please retry with a new idempotency key.");
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

        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            redisTemplate.opsForValue().set(
                IDEMPOTENCY_PREFIX + idempotencyKey,
                payment.getId().toString(),
                IDEMPOTENCY_TTL
            );
        }

        kafkaTemplate.send("payment-events", payment.getId().toString(), payment);

        return CreatePaymentResponse.builder()
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .checkoutUrl("/pay/" + payment.getId())
            .build();
    }

    public PaymentStatusResponse getPaymentStatus(String orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));

        return PaymentStatusResponse.builder()
            .paymentId(payment.getId().toString())
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .currency(payment.getCurrency())
            .status(payment.getStatus().name())
            .failureReason(payment.getFailureReason())
            .build();
    }

    @Transactional
    public void updatePaymentStatus(String paymentId, PaymentStatus newStatus) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.canTransitionTo(newStatus)) {
            throw new RuntimeException(
                "Invalid state transition from " + payment.getStatus() + " to " + newStatus
            );
        }

        payment.setStatus(newStatus);
        paymentRepository.save(payment);

        kafkaTemplate.send("payment-events", payment.getId().toString(), payment);
    }

    @Transactional
    public void verifyOtp(String paymentId, String otp) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
            .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.CHALLENGE_REQUIRED) {
            throw new RuntimeException("Payment is not in CHALLENGE_REQUIRED state");
        }

        if ("123456".equals(otp)) {
            payment.setStatus(PaymentStatus.AUTHORIZED);
            paymentRepository.save(payment);
            kafkaTemplate.send("payment-events", payment.getId().toString(), payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid OTP");
            paymentRepository.save(payment);
            kafkaTemplate.send("payment-events", payment.getId().toString(), payment);
            throw new RuntimeException("Invalid OTP");
        }
    }
}