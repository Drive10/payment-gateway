package dev.payment.paymentservice.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.payment.domain.Order;
import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.PaymentRefund;
import dev.payment.paymentservice.payment.domain.ProcessedWebhookEvent;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.domain.enums.RefundStatus;
import dev.payment.paymentservice.payment.dto.request.RazorpayWebhookRequest;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.integration.client.OrderServiceClient;
import dev.payment.paymentservice.payment.repository.PaymentRefundRepository;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import dev.payment.paymentservice.payment.repository.ProcessedWebhookEventRepository;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@Service
public class RazorpayWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final AuditService auditService;
    private final OrderServiceClient orderServiceClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentTransactionService transactionService;
    private final PaymentStateMachine paymentStateMachine;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayWebhookService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository paymentRefundRepository,
            ProcessedWebhookEventRepository processedWebhookEventRepository,
            AuditService auditService,
            OrderServiceClient orderServiceClient,
            PaymentEventPublisher paymentEventPublisher,
            PaymentTransactionService transactionService,
            PaymentStateMachine paymentStateMachine,
            ObjectMapper objectMapper,
            @Value("${application.webhook.razorpay.secret:}") String webhookSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.processedWebhookEventRepository = processedWebhookEventRepository;
        this.auditService = auditService;
        this.orderServiceClient = orderServiceClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.transactionService = transactionService;
        this.paymentStateMachine = paymentStateMachine;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void processWebhook(String eventId, String signature, String payload) {
        if (eventId == null || eventId.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_WEBHOOK_EVENT_ID", "Webhook event id header is required");
        }
        verifySignature(signature, payload);
        if (processedWebhookEventRepository.existsByEventId(eventId)) {
            return;
        }

        RazorpayWebhookRequest request;
        try {
            request = objectMapper.readValue(payload, RazorpayWebhookRequest.class);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK_PAYLOAD", "Unable to parse Razorpay webhook payload");
        }

        switch (request.event()) {
            case "payment.captured", "payment.authorized" -> processCaptured(request);
            case "payment.failed" -> processFailed(request);
            case "refund.processed" -> processRefund(request);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_WEBHOOK_EVENT", "Unsupported webhook event: " + request.event());
        }

        ProcessedWebhookEvent event = new ProcessedWebhookEvent();
        event.setEventId(eventId);
        event.setEventType(request.event());
        event.setSignature(signature);
        event.setPayloadHash(hashPayload(payload));
        processedWebhookEventRepository.save(event);
    }

    private void processCaptured(RazorpayWebhookRequest request) {
        RazorpayWebhookRequest.EntityWrapper entity = request.payload().payment().entity();
        Payment payment = paymentRepository.findByProviderOrderId(entity.orderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for webhook"));

        // Already captured - skip duplicate
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return;
        }

        // Handle UPI payments that come via "authorized" then "captured"
        // Or direct "captured" for card payments
        if (payment.getStatus() == PaymentStatus.AWAITING_UPI_PAYMENT) {
            paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
        } else if (payment.getStatus() != PaymentStatus.AUTHORIZED && payment.getStatus() != PaymentStatus.PROCESSING) {
            // For other states, need to go through PROCESSING
            paymentStateMachine.transition(payment, PaymentStatus.PROCESSING);
            paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
        }

        payment.setProviderPaymentId(entity.id());
        paymentRepository.save(payment);
        transactionService.createCaptureSuccess(payment, entity.id());
        orderServiceClient.updateOrderStatus(entity.orderId(), "PAID");
        auditService.record("RAZORPAY_WEBHOOK_CAPTURED", "system", "PAYMENT", payment.getId().toString(), "Webhook marked payment captured");
        paymentEventPublisher.publish("payment.webhook.captured", payment, Map.of("providerPaymentId", entity.id()));
    }

    private void processFailed(RazorpayWebhookRequest request) {
        RazorpayWebhookRequest.EntityWrapper entity = request.payload().payment().entity();
        Payment payment = paymentRepository.findByProviderOrderId(entity.orderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for webhook"));

        // Already in final state - skip duplicate
        if (payment.getStatus() == PaymentStatus.CAPTURED 
                || payment.getStatus() == PaymentStatus.FAILED 
                || payment.getStatus() == PaymentStatus.EXPIRED) {
            return;
        }

        // Check if there's error notes in the webhook
        String failureReason = entity.notes() != null && !entity.notes().isEmpty() 
                ? entity.notes() 
                : "Payment failed via webhook";

        paymentStateMachine.transition(payment, PaymentStatus.FAILED);
        payment.setNotes(failureReason);
        paymentRepository.save(payment);
        
        orderServiceClient.updateOrderStatus(entity.orderId(), "FAILED");
        
        auditService.record("RAZORPAY_WEBHOOK_FAILED", "system", "PAYMENT", payment.getId().toString(), failureReason);
        paymentEventPublisher.publish("payment.webhook.failed", payment, Map.of("reason", failureReason));
    }

    private void processRefund(RazorpayWebhookRequest request) {
        RazorpayWebhookRequest.EntityWrapper entity = request.payload().refund().entity();
        if (paymentRefundRepository.findByProviderRefundId(entity.id()).isPresent()) {
            return;
        }

        Payment payment = paymentRepository.findByProviderPaymentId(entity.paymentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for refund webhook"));

        BigDecimal refundAmount = entity.amount() == null ? payment.getAmount() : entity.amount();
        if (payment.getRefundedAmount().add(refundAmount).compareTo(payment.getAmount()) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "REFUND_EXCEEDS_PAYMENT", "Webhook refund exceeds captured amount");
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey("webhook:" + entity.id());
        refund.setRefundReference("webhook_refund_" + entity.id());
        refund.setProviderRefundId(entity.id());
        refund.setAmount(refundAmount);
        refund.setReason(entity.notes());
        refund.setStatus(RefundStatus.PROCESSED);
        paymentRefundRepository.save(refund);

        payment.setRefundedAmount(payment.getRefundedAmount().add(refundAmount));
        paymentStateMachine.transition(payment, payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);
        transactionService.createRefundCompleted(payment, "Webhook processed refund", entity.id(), refundAmount);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            orderServiceClient.updateOrderStatus("ORD-UNKNOWN", "REFUNDED");
        }
        auditService.record("RAZORPAY_WEBHOOK_REFUND", "system", "PAYMENT", payment.getId().toString(), "Webhook processed refund");
        paymentEventPublisher.publish("payment.webhook.refund_processed", payment, Map.of(
                "refundAmount", refundAmount.toPlainString(),
                "refundReference", refund.getRefundReference()
        ));
    }

    private void verifySignature(String signature, String payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WEBHOOK_SECRET_NOT_CONFIGURED", "Webhook secret is not configured");
        }
        if (signature == null || signature.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_WEBHOOK_SIGNATURE", "Webhook signature header is required");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature validation failed");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK_VALIDATION_FAILED", "Unable to validate webhook signature");
        }
    }

    private String hashPayload(String payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PAYLOAD_HASH_FAILED", "Unable to hash webhook payload");
        }
    }
}
