package dev.payment.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.dto.request.RazorpayWebhookRequest;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.repository.PaymentRepository;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

@Service
public class RazorpayWebhookService {

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;
    private final OrderService orderService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public RazorpayWebhookService(
            PaymentRepository paymentRepository,
            AuditService auditService,
            OrderService orderService,
            PaymentEventPublisher paymentEventPublisher,
            PaymentService paymentService,
            ObjectMapper objectMapper,
            @Value("${application.webhook.razorpay.secret:}") String webhookSecret
    ) {
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
        this.orderService = orderService;
        this.paymentEventPublisher = paymentEventPublisher;
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void processWebhook(String signature, String payload) {
        verifySignature(signature, payload);

        RazorpayWebhookRequest request;
        try {
            request = objectMapper.readValue(payload, RazorpayWebhookRequest.class);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_WEBHOOK_PAYLOAD", "Unable to parse Razorpay webhook payload");
        }

        switch (request.event()) {
            case "payment.captured" -> processCaptured(request);
            case "refund.processed" -> processRefund(request);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_WEBHOOK_EVENT", "Unsupported webhook event: " + request.event());
        }
    }

    private void processCaptured(RazorpayWebhookRequest request) {
        RazorpayWebhookRequest.EntityWrapper entity = request.payload().payment().entity();
        Payment payment = paymentRepository.findByProviderOrderId(entity.orderId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for webhook"));

        payment.setProviderPaymentId(entity.id());
        payment.setStatus(PaymentStatus.CAPTURED);
        paymentRepository.save(payment);
        paymentService.createSystemTransaction(payment, dev.payment.paymentservice.domain.enums.TransactionType.WEBHOOK_PROCESSED,
                dev.payment.paymentservice.domain.enums.TransactionStatus.SUCCESS,
                "Webhook marked payment captured", entity.id(), payment.getAmount());
        orderService.markPaid(payment.getOrder());
        auditService.record("RAZORPAY_WEBHOOK_CAPTURED", "system", "PAYMENT", payment.getId().toString(), "Webhook marked payment captured");
        paymentEventPublisher.publish("payment.webhook.captured", payment, Map.of("providerPaymentId", entity.id()));
    }

    private void processRefund(RazorpayWebhookRequest request) {
        RazorpayWebhookRequest.EntityWrapper entity = request.payload().refund().entity();
        Payment payment = paymentRepository.findByProviderPaymentId(entity.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found for refund webhook"));

        BigDecimal refundAmount = entity.amount() == null ? payment.getAmount() : entity.amount();
        payment.setRefundedAmount(payment.getRefundedAmount().add(refundAmount));
        payment.setStatus(payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0 ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);
        paymentService.createSystemTransaction(payment, dev.payment.paymentservice.domain.enums.TransactionType.WEBHOOK_PROCESSED,
                dev.payment.paymentservice.domain.enums.TransactionStatus.SUCCESS,
                "Webhook processed refund", entity.id(), refundAmount);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            orderService.markRefunded(payment.getOrder());
        }
        auditService.record("RAZORPAY_WEBHOOK_REFUND", "system", "PAYMENT", payment.getId().toString(), "Webhook processed refund");
        paymentEventPublisher.publish("payment.webhook.refund_processed", payment, Map.of("refundAmount", refundAmount.toPlainString()));
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
            if (!expected.equalsIgnoreCase(signature)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SIGNATURE", "Webhook signature validation failed");
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK_VALIDATION_FAILED", "Unable to validate webhook signature");
        }
    }
}
