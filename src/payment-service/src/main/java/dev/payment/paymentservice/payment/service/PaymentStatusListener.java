package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.ProcessedWebhookEvent;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.integration.client.OrderServiceClient;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import dev.payment.paymentservice.payment.repository.ProcessedWebhookEventRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
@Profile("!test & !local")
public class PaymentStatusListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusListener.class);

    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final OrderServiceClient orderServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentStateMachine paymentStateMachine;
    private final AuditService auditService;

    public PaymentStatusListener(
            PaymentRepository paymentRepository,
            ProcessedWebhookEventRepository processedWebhookEventRepository,
            OrderServiceClient orderServiceClient,
            PaymentEventPublisher paymentEventPublisher,
            PaymentStateMachine paymentStateMachine,
            AuditService auditService
    ) {
        this.paymentRepository = paymentRepository;
        this.processedWebhookEventRepository = processedWebhookEventRepository;
        this.orderServiceClient = orderServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.paymentStateMachine = paymentStateMachine;
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${application.kafka.topic.webhook-updates}",
            groupId = "payment-service-webhook",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void onWebhookUpdate(ConsumerRecord<String, WebhookUpdateMessage> record) {
        WebhookUpdateMessage message = record.value();
        log.info("event=webhook_update_received paymentId={} status={} eventId={}",
                message.paymentId(), message.status(), message.eventId());

        if (processedWebhookEventRepository.existsByEventId(message.eventId())) {
            log.info("event=webhook_update_duplicate_skipped eventId={}", message.eventId());
            return;
        }

        Optional<Payment> paymentOpt = message.paymentId() != null
                ? paymentRepository.findById(message.paymentId())
                : paymentRepository.findByProviderOrderId(message.providerOrderId());

        if (paymentOpt.isEmpty()) {
            log.warn("event=webhook_update_payment_not_found paymentId={} providerOrderId={}",
                    message.paymentId(), message.providerOrderId());
            return;
        }

        Payment payment = paymentOpt.get();
        PaymentStatus newStatus = PaymentStatus.valueOf(message.status());

        if (payment.getStatus() == newStatus) {
            log.info("event=webhook_update_status_unchanged paymentId={} status={}",
                    payment.getId(), newStatus);
            persistProcessedEvent(message);
            return;
        }

        paymentStateMachine.transition(payment, newStatus);
        paymentRepository.save(payment);

        switch (newStatus) {
            case CAPTURED -> {
                 if (message.providerPaymentId() != null) {
                     payment.setProviderPaymentId(message.providerPaymentId());
                     paymentRepository.save(payment);
                 }
                 updateOrderStatus(payment, "PAID");
                 auditService.record("WEBHOOK_CAPTURED", "system", "PAYMENT", payment.getId().toString(),
                         "Webhook marked payment captured: " + message.providerPaymentId());
                 paymentEventPublisher.publish("payment.webhook.captured", payment, Map.of(
                         "providerPaymentId", message.providerPaymentId() != null ? message.providerPaymentId() : "",
                         "source", "webhook-service"
                 ));
             }
            case FAILED -> {
                 updateOrderStatus(payment, "FAILED");
                 auditService.record("WEBHOOK_FAILED", "system", "PAYMENT", payment.getId().toString(),
                         "Webhook marked payment failed");
                 paymentEventPublisher.publish("payment.webhook.failed", payment, Map.of(
                         "reason", message.reason() != null ? message.reason() : "",
                         "source", "webhook-service"
                 ));
             }
            case REFUNDED, PARTIALLY_REFUNDED -> {
                 if (message.refundAmount() != null) {
                     BigDecimal currentRefunded = payment.getRefundedAmount();
                     payment.setRefundedAmount(currentRefunded.add(message.refundAmount()));
                     paymentRepository.save(payment);
                 }
                 if (newStatus == PaymentStatus.REFUNDED) {
                     updateOrderStatus(payment, "REFUNDED");
                 }
                auditService.record("WEBHOOK_REFUND", "system", "PAYMENT", payment.getId().toString(),
                        "Webhook processed refund");
            }
            default -> log.info("event=webhook_update_status_changed paymentId={} newStatus={}",
                    payment.getId(), newStatus);
        }

        persistProcessedEvent(message);
        log.info("event=webhook_update_processed paymentId={} status={}", payment.getId(), newStatus);
    }

    private void updateOrderStatus(Payment payment, String status) {
        if (payment.getOrder() != null && payment.getOrder().getOrderReference() != null) {
            orderServiceClient.updateOrderStatus(payment.getOrder().getOrderReference(), status);
            return;
        }
        log.warn("event=order_status_update_skipped paymentId={} reason=ORDER_REFERENCE_MISSING targetStatus={}",
                payment.getId(), status);
    }

    private void persistProcessedEvent(WebhookUpdateMessage message) {
        ProcessedWebhookEvent event = new ProcessedWebhookEvent();
        event.setEventId(message.eventId());
        event.setEventType(message.eventType());
        event.setPayloadHash(hashPayload(message.toString()));
        processedWebhookEventRepository.save(event);
    }

    private String hashPayload(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "PAYLOAD_HASH_FAILED", "Unable to hash webhook payload");
        }
    }

    public record WebhookUpdateMessage(
            String eventId,
            String eventType,
            java.util.UUID paymentId,
            String providerOrderId,
            String providerPaymentId,
            String status,
            BigDecimal refundAmount,
            String reason
    ) {
    }
}
