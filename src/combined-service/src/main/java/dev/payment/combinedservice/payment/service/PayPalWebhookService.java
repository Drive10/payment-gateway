package dev.payment.combinedservice.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.combinedservice.payment.domain.Order;
import dev.payment.combinedservice.payment.domain.Payment;
import dev.payment.combinedservice.payment.domain.PaymentRefund;
import dev.payment.combinedservice.payment.domain.ProcessedWebhookEvent;
import dev.payment.combinedservice.payment.domain.enums.PaymentStatus;
import dev.payment.combinedservice.payment.domain.enums.RefundStatus;
import dev.payment.combinedservice.payment.exception.ApiException;
import dev.payment.combinedservice.payment.integration.client.OrderServiceClient;
import dev.payment.combinedservice.payment.repository.PaymentRefundRepository;
import dev.payment.combinedservice.payment.repository.PaymentRepository;
import dev.payment.combinedservice.payment.repository.ProcessedWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Service
public class PayPalWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PayPalWebhookService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final AuditService auditService;
    private final OrderServiceClient orderServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentTransactionService transactionService;
    private final PaymentStateMachine paymentStateMachine;
    private final PaymentMetricsService metricsService;
    private final ObjectMapper objectMapper;
    private final String webhookId;

    public PayPalWebhookService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository paymentRefundRepository,
            ProcessedWebhookEventRepository processedWebhookEventRepository,
            AuditService auditService,
            OrderServiceClient orderServiceClient,
            PaymentEventPublisher paymentEventPublisher,
            PaymentTransactionService transactionService,
            PaymentStateMachine paymentStateMachine,
            PaymentMetricsService metricsService,
            ObjectMapper objectMapper,
            @Value("${application.webhook.paypal.webhook-id:}") String webhookId
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.processedWebhookEventRepository = processedWebhookEventRepository;
        this.auditService = auditService;
        this.orderServiceClient = orderServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.transactionService = transactionService;
        this.paymentStateMachine = paymentStateMachine;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
        this.webhookId = webhookId;
    }

    @Transactional
    public void processWebhook(String eventId, String transmissionId, String signature, String certUrl, String payload) {
        metricsService.recordWebhookReceived();

        validateConfig();

        if (processedWebhookEventRepository.existsByEventId(eventId)) {
            log.info("event=paypal_webhook_duplicate eventId={}", eventId);
            metricsService.recordWebhookDuplicated();
            return;
        }

        try {
            Map<String, Object> webhookEvent = objectMapper.readValue(payload, Map.class);
            String eventType = (String) webhookEvent.get("event_type");

            switch (eventType) {
                case "PAYMENT.CAPTURE.COMPLETED" -> processCaptureCompleted(webhookEvent);
                case "PAYMENT.CAPTURE.DENIED" -> processCaptureDenied(webhookEvent);
                case "PAYMENT.CAPTURE.REFUNDED" -> processCaptureRefunded(webhookEvent);
                case "PAYMENT.CAPTURE.PENDING" -> processCapturePending(webhookEvent);
                case "CHECKOUT.ORDER.COMPLETED" -> processOrderCompleted(webhookEvent);
                default -> log.debug("event=paypal_unhandled_event type={}", eventType);
            }

            ProcessedWebhookEvent event = new ProcessedWebhookEvent();
            event.setEventId(eventId);
            event.setEventType(eventType);
            event.setSignature(transmissionId);
            event.setPayloadHash(hashPayload(payload));
            processedWebhookEventRepository.save(event);

            metricsService.recordWebhookProcessed();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYPAL_WEBHOOK_PAYLOAD", "Unable to parse PayPal webhook payload");
        }
    }

    @SuppressWarnings("unchecked")
    private void processCaptureCompleted(Map<String, Object> webhookEvent) {
        Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
        String captureId = (String) resource.get("id");
        String orderId = extractOrderId(resource);
        String customId = resource.containsKey("custom_id") ? (String) resource.get("custom_id") : null;

        Payment payment = customId != null
                ? findPaymentByCustomId(customId)
                : findPayment(orderId, captureId);

        if (payment == null) {
            log.warn("event=paypal_payment_not_found captureId={} orderId={}", captureId, orderId);
            return;
        }

        if (isTerminalState(payment.getStatus())) {
            return;
        }

        paymentStateMachine.transition(payment, PaymentStatus.PROCESSING);
        paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
        payment.setProviderPaymentId(captureId);
        paymentRepository.save(payment);

        transactionService.createCaptureSuccess(payment, captureId);
        orderServiceClient.updateOrderStatus("ORD-UNKNOWN", "PAID");
        auditService.record("PAYPAL_WEBHOOK_CAPTURED", "system", "PAYMENT", payment.getId().toString(), "PayPal webhook captured payment");
        paymentEventPublisher.publish("payment.webhook.captured", payment, Map.of("providerPaymentId", captureId));

        metricsService.recordPaymentCaptured();
        log.info("event=paypal_payment_captured paymentId={} captureId={}", payment.getId(), captureId);
    }

    @SuppressWarnings("unchecked")
    private void processCaptureDenied(Map<String, Object> webhookEvent) {
        Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
        String captureId = (String) resource.get("id");
        String orderId = extractOrderId(resource);

        Payment payment = findPayment(orderId, captureId);
        if (payment == null || isTerminalState(payment.getStatus())) {
            return;
        }

        String reason = extractDenialReason(resource);
        paymentStateMachine.transition(payment, PaymentStatus.FAILED);
        payment.setNotes(reason);
        paymentRepository.save(payment);

        orderServiceClient.updateOrderStatus("ORD-UNKNOWN", "FAILED");
        auditService.record("PAYPAL_WEBHOOK_DENIED", "system", "PAYMENT", payment.getId().toString(), reason);
        paymentEventPublisher.publish("payment.webhook.failed", payment, Map.of("reason", reason));

        metricsService.recordPaymentFailed();
        log.info("event=paypal_payment_denied paymentId={} reason={}", payment.getId(), reason);
    }

    @SuppressWarnings("unchecked")
    private void processCaptureRefunded(Map<String, Object> webhookEvent) {
        Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
        String captureId = (String) resource.get("id");
        String refundId = resource.containsKey("refund_id") ? (String) resource.get("refund_id") : null;

        Payment payment = paymentRepository.findByProviderPaymentId(captureId).orElse(null);
        if (payment == null) {
            log.warn("event=paypal_refund_payment_not_found captureId={}", captureId);
            return;
        }

        Map<String, Object> amountMap = (Map<String, Object>) resource.get("amount");
        BigDecimal refundedAmount = amountMap != null
                ? new BigDecimal((String) amountMap.get("value"))
                : payment.getAmount();

        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey("paypal_refund:" + (refundId != null ? refundId : captureId));
        refund.setRefundReference("paypal_refund_" + (refundId != null ? refundId : captureId));
        refund.setProviderRefundId(refundId != null ? refundId : captureId);
        refund.setAmount(refundedAmount);
        refund.setReason("PayPal webhook refund");
        refund.setStatus(RefundStatus.PROCESSED);
        paymentRefundRepository.save(refund);

        payment.setRefundedAmount(refundedAmount);
        PaymentStatus newStatus = payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        paymentStateMachine.transition(payment, newStatus);
        paymentRepository.save(payment);

        transactionService.createRefundCompleted(payment, "PayPal webhook refund", refundId, refundedAmount);
        auditService.record("PAYPAL_WEBHOOK_REFUND", "system", "PAYMENT", payment.getId().toString(), "Refund processed via PayPal webhook");

        metricsService.recordPaymentRefunded();
        log.info("event=paypal_refund_processed paymentId={} amount={}", payment.getId(), refundedAmount);
    }

    @SuppressWarnings("unchecked")
    private void processCapturePending(Map<String, Object> webhookEvent) {
        Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
        String captureId = (String) resource.get("id");
        String orderId = extractOrderId(resource);

        Payment payment = findPayment(orderId, captureId);
        if (payment == null) {
            return;
        }

        if (payment.getStatus() == PaymentStatus.AUTHORIZATION_PENDING) {
            paymentStateMachine.transition(payment, PaymentStatus.AUTHORIZATION_PENDING);
            paymentRepository.save(payment);
            log.info("event=paypal_payment_pending paymentId={}", payment.getId());
        }
    }

    @SuppressWarnings("unchecked")
    private void processOrderCompleted(Map<String, Object> webhookEvent) {
        Map<String, Object> resource = (Map<String, Object>) webhookEvent.get("resource");
        String orderId = (String) resource.get("id");

        if (resource.containsKey("purchase_units")) {
            var purchaseUnits = (java.util.List<Map<String, Object>>) resource.get("purchase_units");
            if (!purchaseUnits.isEmpty()) {
                var payments = (Map<String, Object>) purchaseUnits.get(0).get("payments");
                if (payments != null && payments.containsKey("captures")) {
                    var captures = (java.util.List<Map<String, Object>>) payments.get("captures");
                    if (!captures.isEmpty()) {
                        String captureId = (String) captures.get(0).get("id");
                        processCaptureCompleted(webhookEvent);
                    }
                }
            }
        }
    }

    private Payment findPayment(String orderId, String captureId) {
        return paymentRepository.findByProviderOrderId(orderId)
                .orElseGet(() -> paymentRepository.findByProviderPaymentId(captureId).orElse(null));
    }

    private Payment findPaymentByCustomId(String customId) {
        return paymentRepository.findAll().stream()
                .filter(p -> customId.equals(p.getIdempotencyKey()))
                .findFirst()
                .orElse(null);
    }

    private String extractOrderId(Map<String, Object> resource) {
        if (resource.containsKey("supplementary_data")) {
            Map<String, Object> supplementaryData = (Map<String, Object>) resource.get("supplementary_data");
            if (supplementaryData.containsKey("related_ids")) {
                Map<String, Object> relatedIds = (Map<String, Object>) supplementaryData.get("related_ids");
                if (relatedIds.containsKey("order_id")) {
                    return (String) relatedIds.get("order_id");
                }
            }
        }
        return resource.containsKey("custom_id") ? (String) resource.get("custom_id") : null;
    }

    private String extractDenialReason(Map<String, Object> resource) {
        if (resource.containsKey("status_details")) {
            Map<String, Object> statusDetails = (Map<String, Object>) resource.get("status_details");
            return (String) statusDetails.getOrDefault("reason", "Payment denied by PayPal");
        }
        return "Payment denied by PayPal";
    }

    private boolean isTerminalState(PaymentStatus status) {
        return status == PaymentStatus.CAPTURED
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.REFUNDED;
    }

    private void validateConfig() {
        if (webhookId == null || webhookId.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WEBHOOK_ID_NOT_CONFIGURED", "PayPal webhook ID is not configured");
        }
    }

    private String hashPayload(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_HASH_FAILED", "Unable to hash webhook payload");
        }
    }
}