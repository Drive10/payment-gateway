package dev.payment.combinedservice.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.combinedservice.payment.domain.Order;
import dev.payment.combinedservice.payment.domain.Payment;
import dev.payment.combinedservice.payment.domain.PaymentRefund;
import dev.payment.combinedservice.payment.domain.ProcessedWebhookEvent;
import dev.payment.combinedservice.payment.domain.enums.PaymentStatus;
import dev.payment.combinedservice.payment.domain.enums.RefundStatus;
import dev.payment.combinedservice.payment.dto.request.StripeWebhookRequest;
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
import java.util.HexFormat;
import java.util.Map;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

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
    private final String webhookSecret;

    public StripeWebhookService(
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
            @Value("${application.webhook.stripe.secret:}") String webhookSecret
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
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void processWebhook(String eventId, String signatureHeader, String payload) {
        metricsService.recordWebhookReceived();

        validateConfig();

        String signature = parseSignature(signatureHeader);
        verifySignature(signature, payload);

        if (processedWebhookEventRepository.existsByEventId(eventId)) {
            log.info("event=stripe_webhook_duplicate eventId={}", eventId);
            metricsService.recordWebhookDuplicated();
            return;
        }

        StripeWebhookRequest request = parsePayload(payload);

        switch (request.type()) {
            case "payment_intent.succeeded" -> processPaymentSuccess(request);
            case "payment_intent.payment_failed" -> processPaymentFailed(request);
            case "charge.captured" -> processChargeCaptured(request);
            case "charge.refunded" -> processChargeRefunded(request);
            case "payment_method.attached" -> log.debug("event=stripe_payment_method_attached paymentMethodId={}", request.data().object().id());
            default -> log.debug("event=stripe_unhandled_event type={}", request.type());
        }

        ProcessedWebhookEvent event = new ProcessedWebhookEvent();
        event.setEventId(eventId);
        event.setEventType(request.type());
        event.setSignature(signature);
        event.setPayloadHash(hashPayload(payload));
        processedWebhookEventRepository.save(event);

        metricsService.recordWebhookProcessed();
    }

    private void processPaymentSuccess(StripeWebhookRequest request) {
        String pi = resolvePaymentIntent(request);

        Payment payment = findPayment(pi);
        if (payment == null) {
            log.warn("event=stripe_payment_not_found paymentIntentId={}", pi);
            return;
        }

        if (isTerminalState(payment.getStatus())) {
            return;
        }

        paymentStateMachine.transition(payment, PaymentStatus.PROCESSING);
        paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
        payment.setProviderPaymentId(pi);
        paymentRepository.save(payment);

        transactionService.createCaptureSuccess(payment, pi);
        orderServiceClient.updateOrderStatus("ORD-UNKNOWN", "PAID");
        auditService.record("STRIPE_WEBHOOK_CAPTURED", "system", "PAYMENT", payment.getId().toString(), "Stripe webhook captured payment");
        paymentEventPublisher.publish("payment.webhook.captured", payment, Map.of("providerPaymentId", pi));

        metricsService.recordPaymentCaptured();
        log.info("event=stripe_payment_captured paymentId={} providerPaymentId={}", payment.getId(), pi);
    }

    private void processPaymentFailed(StripeWebhookRequest request) {
        String pi = resolvePaymentIntent(request);

        Payment payment = findPayment(pi);
        if (payment == null) {
            log.warn("event=stripe_payment_not_found paymentIntentId={}", pi);
            return;
        }

        if (isTerminalState(payment.getStatus())) {
            return;
        }

        String failureReason = extractFailureReason(request.data().object());
        paymentStateMachine.transition(payment, PaymentStatus.FAILED);
        payment.setNotes(failureReason);
        paymentRepository.save(payment);

        orderServiceClient.updateOrderStatus("ORD-UNKNOWN", "FAILED");
        auditService.record("STRIPE_WEBHOOK_FAILED", "system", "PAYMENT", payment.getId().toString(), failureReason);
        paymentEventPublisher.publish("payment.webhook.failed", payment, Map.of("reason", failureReason));

        metricsService.recordPaymentFailed();
        log.info("event=stripe_payment_failed paymentId={} reason={}", payment.getId(), failureReason);
    }

    private void processChargeCaptured(StripeWebhookRequest request) {
        String chargeId = request.data().object().id();
        String pi = request.data().object().paymentIntent();

        Payment payment = findPayment(pi);
        if (payment == null || isTerminalState(payment.getStatus())) {
            return;
        }

        paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
        payment.setProviderPaymentId(chargeId);
        paymentRepository.save(payment);

        transactionService.createCaptureSuccess(payment, chargeId);
        metricsService.recordPaymentCaptured();
    }

    private void processChargeRefunded(StripeWebhookRequest request) {
        String paymentIntentId = request.data().object().paymentIntent();
        Payment payment = findPayment(paymentIntentId);

        if (payment == null) {
            log.warn("event=stripe_refund_payment_not_found paymentIntentId={}", paymentIntentId);
            return;
        }

        Long refundAmount = request.data().object().amountRefunded();
        BigDecimal refundedAmount = refundAmount != null
                ? BigDecimal.valueOf(refundAmount).divide(BigDecimal.valueOf(100))
                : payment.getAmount();

        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey("stripe_refund:" + request.data().object().id());
        refund.setRefundReference("stripe_refund_" + request.data().object().id());
        refund.setProviderRefundId(request.data().object().id());
        refund.setAmount(refundedAmount);
        refund.setReason(request.data().object().reason());
        refund.setStatus(RefundStatus.PROCESSED);
        paymentRefundRepository.save(refund);

        payment.setRefundedAmount(refundedAmount);
        PaymentStatus newStatus = payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        paymentStateMachine.transition(payment, newStatus);
        paymentRepository.save(payment);

        transactionService.createRefundCompleted(payment, "Stripe webhook refund", request.data().object().id(), refundedAmount);
        auditService.record("STRIPE_WEBHOOK_REFUND", "system", "PAYMENT", payment.getId().toString(), "Refund processed via Stripe webhook");

        metricsService.recordPaymentRefunded();
        log.info("event=stripe_refund_processed paymentId={} amount={}", payment.getId(), refundedAmount);
    }

    private String resolvePaymentIntent(StripeWebhookRequest request) {
        String pi = request.data().object().paymentIntent();
        return pi != null ? pi : request.data().object().id();
    }

    private Payment findPayment(String paymentIntentId) {
        return paymentRepository.findByProviderPaymentId(paymentIntentId)
                .orElseGet(() -> paymentRepository.findByProviderOrderId(paymentIntentId).orElse(null));
    }

    private boolean isTerminalState(PaymentStatus status) {
        return status == PaymentStatus.CAPTURED 
                || status == PaymentStatus.FAILED 
                || status == PaymentStatus.REFUNDED;
    }

    private void validateConfig() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WEBHOOK_SECRET_NOT_CONFIGURED", "Stripe webhook secret is not configured");
        }
    }

    private String parseSignature(String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_WEBHOOK_SIGNATURE", "Stripe-Signature header is required");
        }
        return signatureHeader;
    }

    private void verifySignature(String signature, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);

            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Stripe webhook signature validation failed");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK_VALIDATION_FAILED", "Unable to validate Stripe webhook signature");
        }
    }

    private StripeWebhookRequest parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, StripeWebhookRequest.class);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK_PAYLOAD", "Unable to parse Stripe webhook payload");
        }
    }

    private String extractFailureReason(StripeWebhookRequest.StripeObject object) {
        if (object.paymentMethodDetails() != null) {
            Object card = object.paymentMethodDetails().get("card");
            if (card instanceof Map) {
                Map<?, ?> cardDetails = (Map<?, ?>) card;
                String declineCode = (String) cardDetails.get("decline_code");
                if (declineCode != null) {
                    return "Stripe decline: " + declineCode;
                }
            }
        }
        return object.metadata() != null ? object.metadata().get("failure_reason") : "Payment failed via Stripe webhook";
    }

    private String hashPayload(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_HASH_FAILED", "Unable to hash webhook payload");
        }
    }
}