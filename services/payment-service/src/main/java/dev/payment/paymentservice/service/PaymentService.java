package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentRefund;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.domain.enums.RefundStatus;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.dto.FeeCalculation;
import dev.payment.paymentservice.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.dto.request.CreateRefundRequest;
import dev.payment.paymentservice.dto.response.PaymentResponse;
import dev.payment.paymentservice.dto.response.RefundResponse;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.integration.processor.PaymentProcessorCaptureResponse;
import dev.payment.paymentservice.integration.processor.PaymentProcessorClient;
import dev.payment.paymentservice.integration.processor.PaymentProcessorIntentResponse;
import dev.payment.paymentservice.mapper.PaymentMapper;
import dev.payment.paymentservice.repository.PaymentRefundRepository;
import dev.payment.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String SIMULATED_PROVIDER = "RAZORPAY_SIMULATOR";

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final OrderService orderService;
    private final AuditService auditService;
    private final PaymentProcessorClient paymentProcessorClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final IdempotencyService idempotencyService;
    private final PaymentStateMachine paymentStateMachine;
    private final FeeEngine feeEngine;
    private final LedgerService ledgerService;
    private final PaymentTransactionService transactionService;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository paymentRefundRepository,
            OrderService orderService,
            AuditService auditService,
            PaymentProcessorClient paymentProcessorClient,
            PaymentEventPublisher paymentEventPublisher,
            IdempotencyService idempotencyService,
            PaymentStateMachine paymentStateMachine,
            FeeEngine feeEngine,
            LedgerService ledgerService,
            PaymentTransactionService transactionService,
            PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.orderService = orderService;
        this.auditService = auditService;
        this.paymentProcessorClient = paymentProcessorClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.idempotencyService = idempotencyService;
        this.paymentStateMachine = paymentStateMachine;
        this.feeEngine = feeEngine;
        this.ledgerService = ledgerService;
        this.transactionService = transactionService;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, User actor) {
        IdempotencyService.IdempotencyResult<PaymentResponse> idempotency =
                idempotencyService.begin("PAYMENT_CREATE", idempotencyKey, actorIdKey(actor), Map.of(
                        "actor", actor.getEmail(),
                        "request", request
                ), PaymentResponse.class);
        if (idempotency.replayed()) {
            return idempotency.cachedResponse();
        }

        Payment payment = buildPayment(request, idempotencyKey);
        Payment savedPayment = executePaymentCreation(payment, request, actor, idempotencyKey);

        PaymentResponse response = paymentMapper.toResponse(savedPayment, transactionService.findByPaymentId(savedPayment.getId()));
        idempotencyService.complete(idempotency.record(), response, savedPayment.getId().toString());
        return response;
    }

    private Payment buildPayment(CreatePaymentRequest request, String idempotencyKey) {
        Payment payment = new Payment();
        payment.setIdempotencyKey(idempotencyKey);
        payment.setProvider(normalizeProvider(request.provider()));
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTransactionMode(resolveMode(request.transactionMode()));
        payment.setNotes(request.notes());
        payment.setMerchantId(request.merchantId());
        payment.setPricingTier("STANDARD");
        return payment;
    }

    private void enrichPaymentFromOrder(Payment payment, Order order) {
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
    }

    private Payment executePaymentCreation(Payment payment, CreatePaymentRequest request, User actor, String idempotencyKey) {
        try {
            Order order = orderService.getOwnedOrder(request.orderId(), actor, false);
            payment.setOrderId(order.getId());
            payment.setOrder(order);
            enrichPaymentFromOrder(payment, order);

            PaymentProcessorIntentResponse processorIntent = paymentProcessorClient.createIntent(
                    payment, order.getOrderReference(), payment.getTransactionMode());
            if (processorIntent == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "PROCESSOR_EMPTY_RESPONSE", "Payment processor returned no intent");
            }
            payment.setProviderOrderId(processorIntent.providerOrderId());
            payment.setCheckoutUrl(processorIntent.checkoutUrl());
            payment.setSimulated(processorIntent.simulated());

            return savePayment(payment, idempotencyKey);
        } catch (DataIntegrityViolationException exception) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
        }
    }

    private Payment savePayment(Payment payment, String idempotencyKey) {
        Payment saved = paymentRepository.save(payment);
        transactionService.createPaymentInitiated(saved);
        orderService.markPaymentPending(payment.getOrderId(), payment.getOrder().getOrderReference());
        return saved;
    }

    @Transactional
    public PaymentResponse capturePayment(UUID paymentId, CapturePaymentRequest request, User actor) {
        Payment payment = getOwnedPayment(paymentId, actor);
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return paymentMapper.toResponse(payment, transactionService.findByPaymentId(paymentId));
        }

        paymentStateMachine.transition(payment, PaymentStatus.PROCESSING);

        try {
            PaymentProcessorCaptureResponse processorCapture = paymentProcessorClient.capture(
                    payment, request, payment.getTransactionMode());
            payment.setProviderPaymentId(processorCapture.providerPaymentId());
            payment.setProviderSignature(processorCapture.providerSignature());
            payment.setSimulated(processorCapture.simulated());

            calculateAndSetFees(payment);
            paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
            paymentRepository.save(payment);

            recordToLedger(payment);
            transactionService.createCaptureSuccess(payment, processorCapture.providerReference());
            updateOrderStatus(payment);
            auditService.record("PAYMENT_CAPTURED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "Payment captured successfully");
            paymentEventPublisher.publish("payment.captured", payment, Map.of("actor", actor.getEmail()));

            log.info("event=payment_captured paymentId={} actor={} mode={} simulated={}",
                    payment.getId(), actor.getEmail(), payment.getTransactionMode(), payment.isSimulated());

            return paymentMapper.toResponse(payment, transactionService.findByPaymentId(paymentId));
        } catch (RuntimeException exception) {
            handleCaptureFailure(payment, actor);
            throw exception;
        }
    }

    private void calculateAndSetFees(Payment payment) {
        FeeCalculation fees = feeEngine.calculateFees(
                payment.getMerchantId(),
                payment.getAmount(),
                payment.getPricingTier(),
                payment.getMethod().name()
        );
        payment.setPlatformFee(fees.platformFee());
        payment.setGatewayFee(fees.gatewayFee());
    }

    private void recordToLedger(Payment payment) {
        try {
            ledgerService.recordPayment(
                    payment.getId(),
                    payment.getId().toString(),
                    payment.getMerchantId(),
                    payment.getAmount(),
                    payment.getPlatformFee(),
                    payment.getGatewayFee(),
                    payment.getCurrency()
            );
        } catch (Exception ledgerException) {
            log.warn("event=ledger_recording_failed paymentId={} error={}", payment.getId(), ledgerException.getMessage());
        }
    }

    private void handleCaptureFailure(Payment payment, User actor) {
        if (payment.getStatus() != PaymentStatus.CAPTURED) {
            paymentStateMachine.transition(payment, PaymentStatus.FAILED);
        }
        paymentRepository.save(payment);
        transactionService.createCaptureFailed(payment);
        updateOrderStatusOnFailure(payment);
    }

    @Transactional
    public RefundResponse refundPayment(UUID paymentId, CreateRefundRequest request, String idempotencyKey, User actor) {
        IdempotencyService.IdempotencyResult<RefundResponse> idempotency =
                idempotencyService.begin("PAYMENT_REFUND", idempotencyKey, actorIdKey(actor), Map.of(
                        "actor", actor.getEmail(),
                        "paymentId", paymentId,
                        "request", request
                ), RefundResponse.class);
        if (idempotency.replayed()) {
            return idempotency.cachedResponse();
        }

        Payment payment = getOwnedPayment(paymentId, actor);
        validateRefundability(payment, request.amount());
        BigDecimal alreadyRefunded = paymentRefundRepository.sumRefundedAmountByPaymentId(payment.getId());

        PaymentRefund refund = createRefund(payment, request, idempotencyKey);
        transactionService.createRefundRequested(payment, request.reason(), refund.getRefundReference(), request.amount());

        refund.setStatus(RefundStatus.PROCESSED);
        paymentRefundRepository.save(refund);

        updatePaymentAfterRefund(payment, alreadyRefunded, request.amount());
        transactionService.createRefundCompleted(payment, request.reason(), refund.getRefundReference(), request.amount());
        recordRefundToLedger(payment, refund, request.amount());
        publishRefundEvents(payment, actor, refund, request);

        RefundResponse response = paymentMapper.toRefundResponse(refund);
        idempotencyService.complete(idempotency.record(), response, refund.getId().toString());
        return response;
    }

    private void validateRefundability(Payment payment, BigDecimal refundAmount) {
        if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_NOT_REFUNDABLE", "Only captured payments can be refunded");
        }
        BigDecimal alreadyRefunded = paymentRefundRepository.sumRefundedAmountByPaymentId(payment.getId());
        if (alreadyRefunded.add(refundAmount).compareTo(payment.getAmount()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REFUND_EXCEEDS_PAYMENT", "Refund amount exceeds captured payment amount");
        }
    }

    private PaymentRefund createRefund(Payment payment, CreateRefundRequest request, String idempotencyKey) {
        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setRefundReference("refund_" + UUID.randomUUID().toString().replace("-", ""));
        refund.setProviderRefundId(refund.getRefundReference());
        refund.setAmount(request.amount());
        refund.setReason(request.reason());
        refund.setStatus(RefundStatus.REQUESTED);
        return paymentRefundRepository.save(refund);
    }

    private void updatePaymentAfterRefund(Payment payment, BigDecimal alreadyRefunded, BigDecimal refundAmount) {
        payment.setRefundedAmount(alreadyRefunded.add(refundAmount));
        PaymentStatus newStatus = payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
        paymentStateMachine.transition(payment, newStatus);
        paymentRepository.save(payment);

        if (newStatus == PaymentStatus.REFUNDED) {
            orderService.markRefunded(payment.getOrderId(), getOrderReference(payment));
        }
    }

    private void recordRefundToLedger(Payment payment, PaymentRefund refund, BigDecimal refundAmount) {
        FeeCalculation refundFees = feeEngine.calculateRefundFees(
                payment.getMerchantId(),
                payment.getAmount(),
                refundAmount,
                payment.getPricingTier()
        );
        ledgerService.recordRefund(
                refund.getId(),
                refund.getRefundReference(),
                payment.getId(),
                payment.getId().toString(),
                payment.getMerchantId(),
                refundAmount,
                refundFees.platformFee(),
                payment.getCurrency()
        );
    }

    private void publishRefundEvents(Payment payment, User actor, PaymentRefund refund, CreateRefundRequest request) {
        auditService.record("PAYMENT_REFUNDED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "Refund processed");
        paymentEventPublisher.publish("payment.refunded", payment, Map.of(
                "actor", actor.getEmail(),
                "refundId", refund.getId().toString(),
                "refundReference", refund.getRefundReference(),
                "refundAmount", request.amount().toPlainString(),
                "reason", request.reason() != null ? request.reason() : ""
        ));
    }

    private void updateOrderStatus(Payment payment) {
        try {
            orderService.markPaid(payment.getOrderId(), getOrderReference(payment));
        } catch (Exception orderException) {
            log.warn("event=order_status_update_failed paymentId={} error={}", payment.getId(), orderException.getMessage());
        }
    }

    private void updateOrderStatusOnFailure(Payment payment) {
        try {
            orderService.markFailed(payment.getOrderId(), getOrderReference(payment));
        } catch (Exception orderException) {
            log.warn("event=order_status_update_failed paymentId={} error={}", payment.getId(), orderException.getMessage());
        }
    }

    private String getOrderReference(Payment payment) {
        return payment.getOrder() != null ? payment.getOrder().getOrderReference() : "ORD-UNKNOWN";
    }

    private Payment getOwnedPayment(UUID paymentId, User actor) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
    }

    private long actorIdKey(User actor) {
        return Math.abs(actor.getId().getLeastSignificantBits());
    }

    private TransactionMode resolveMode(TransactionMode requestedMode) {
        return requestedMode == null ? TransactionMode.PRODUCTION : requestedMode;
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase();
        if (normalized.isBlank()) {
            return SIMULATED_PROVIDER;
        }
        return switch (normalized) {
            case "STRIPE", "RAZORPAY", "PAYPAL" -> SIMULATED_PROVIDER;
            default -> normalized;
        };
    }

    private String resolveSimulationMode(Payment payment) {
        if (payment.getTransactionMode() == TransactionMode.TEST) {
            return "TEST";
        }
        return "SUCCESS";
    }
}
