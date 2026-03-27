package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentRefund;
import dev.payment.paymentservice.domain.PaymentTransaction;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.domain.enums.RefundStatus;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.domain.enums.TransactionStatus;
import dev.payment.paymentservice.domain.enums.TransactionType;
import dev.payment.paymentservice.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.dto.request.CreateRefundRequest;
import dev.payment.paymentservice.dto.response.PaymentResponse;
import dev.payment.paymentservice.dto.response.RefundResponse;
import dev.payment.paymentservice.dto.response.PaymentTransactionResponse;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.integration.processor.PaymentProcessorCaptureResponse;
import dev.payment.paymentservice.integration.processor.PaymentProcessorClient;
import dev.payment.paymentservice.integration.processor.PaymentProcessorIntentResponse;
import dev.payment.paymentservice.repository.PaymentRefundRepository;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.PaymentTransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String SIMULATED_PROVIDER = "RAZORPAY_SIMULATOR";

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderService orderService;
    private final AuditService auditService;
    private final PaymentProcessorClient paymentProcessorClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final IdempotencyService idempotencyService;
    private final PaymentStateMachine paymentStateMachine;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository paymentRefundRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderService orderService,
            AuditService auditService,
            PaymentProcessorClient paymentProcessorClient,
            PaymentEventPublisher paymentEventPublisher,
            IdempotencyService idempotencyService,
            PaymentStateMachine paymentStateMachine
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderService = orderService;
        this.auditService = auditService;
        this.paymentProcessorClient = paymentProcessorClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.idempotencyService = idempotencyService;
        this.paymentStateMachine = paymentStateMachine;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, User actor, boolean adminView) {
        IdempotencyService.IdempotencyResult<PaymentResponse> idempotency =
                idempotencyService.begin("PAYMENT_CREATE", idempotencyKey, actor.getId(), Map.of(
                        "actor", actor.getEmail(),
                        "request", request
                ), PaymentResponse.class);
        if (idempotency.replayed()) {
            return idempotency.cachedResponse();
        }

        Order order = orderService.getOwnedOrder(request.orderId(), actor, adminView);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setProvider(normalizeProvider(request.provider()));
        payment.setMethod(request.method());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setTransactionMode(resolveMode(request.transactionMode()));
        payment.setIdempotencyKey(idempotencyKey);
        payment.setNotes(request.notes());

        PaymentProcessorIntentResponse processorIntent = paymentProcessorClient.createIntent(payment, order.getOrderReference(), payment.getTransactionMode());
        payment.setProviderOrderId(processorIntent.providerOrderId());
        payment.setCheckoutUrl(processorIntent.checkoutUrl());
        payment.setSimulated(processorIntent.simulated());
        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException exception) {
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
            return toResponse(existing);
        }

        createTransaction(
                payment,
                TransactionType.PAYMENT_INITIATED,
                TransactionStatus.PENDING,
                payment.getTransactionMode() == TransactionMode.TEST ? "Test simulator order created" : "Production payment order created",
                payment.getProviderOrderId()
        );
        orderService.markPaymentPending(order);
        auditService.record("PAYMENT_CREATED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "Payment initiated for order " + order.getOrderReference());
        paymentEventPublisher.publish("payment.created", payment, Map.of("actor", actor.getEmail()));
        log.info("event=payment_created paymentId={} orderId={} actor={} provider={} mode={} simulated={}",
                payment.getId(), order.getId(), actor.getEmail(), payment.getProvider(), payment.getTransactionMode(), payment.isSimulated());
        PaymentResponse response = toResponse(payment);
        idempotencyService.complete(idempotency.record(), response, payment.getId().toString());
        return response;
    }

    @Transactional
    public PaymentResponse capturePayment(UUID paymentId, CapturePaymentRequest request, User actor, boolean adminView) {
        Payment payment = getOwnedPayment(paymentId, actor, adminView);
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return toResponse(payment);
        }

        paymentStateMachine.transition(payment, PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try {
            PaymentProcessorCaptureResponse processorCapture = paymentProcessorClient.capture(payment, request, payment.getTransactionMode());
            payment.setProviderPaymentId(processorCapture.providerPaymentId());
            payment.setProviderSignature(processorCapture.providerSignature());
            payment.setSimulated(processorCapture.simulated());
            paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
            paymentRepository.save(payment);

            createTransaction(
                    payment,
                    TransactionType.PAYMENT_CAPTURED,
                    TransactionStatus.SUCCESS,
                    payment.getTransactionMode() == TransactionMode.TEST ? "Test payment captured" : "Production payment captured",
                    processorCapture.providerReference()
            );
            orderService.markPaid(payment.getOrder());
            auditService.record("PAYMENT_CAPTURED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "Payment captured successfully");
            paymentEventPublisher.publish("payment.captured", payment, Map.of("actor", actor.getEmail()));
            log.info("event=payment_captured paymentId={} actor={} mode={} simulated={}", payment.getId(), actor.getEmail(), payment.getTransactionMode(), payment.isSimulated());
            return toResponse(payment);
        } catch (RuntimeException exception) {
            paymentStateMachine.transition(payment, PaymentStatus.FAILED);
            paymentRepository.save(payment);
            orderService.markFailed(payment.getOrder());
            createTransaction(
                    payment,
                    TransactionType.PAYMENT_CAPTURED,
                    TransactionStatus.FAILED,
                    "Payment capture failed",
                    payment.getProviderOrderId()
            );
            throw exception;
        }
    }

    @Transactional
    public RefundResponse refundPayment(UUID paymentId, CreateRefundRequest request, String idempotencyKey, User actor, boolean adminView) {
        IdempotencyService.IdempotencyResult<RefundResponse> idempotency =
                idempotencyService.begin("PAYMENT_REFUND", idempotencyKey, actor.getId(), Map.of(
                        "actor", actor.getEmail(),
                        "paymentId", paymentId,
                        "request", request
                ), RefundResponse.class);
        if (idempotency.replayed()) {
            return idempotency.cachedResponse();
        }

        Payment payment = getOwnedPayment(paymentId, actor, adminView);
        if (payment.getStatus() != PaymentStatus.CAPTURED && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ApiException(HttpStatus.CONFLICT, "PAYMENT_NOT_REFUNDABLE", "Only captured payments can be refunded");
        }

        java.math.BigDecimal alreadyRefunded = paymentRefundRepository.sumRefundedAmountByPaymentId(payment.getId());
        if (alreadyRefunded.add(request.amount()).compareTo(payment.getAmount()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REFUND_EXCEEDS_PAYMENT", "Refund amount exceeds captured payment amount");
        }

        PaymentRefund refund = new PaymentRefund();
        refund.setPayment(payment);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setRefundReference("refund_" + UUID.randomUUID().toString().replace("-", ""));
        refund.setProviderRefundId(refund.getRefundReference());
        refund.setAmount(request.amount());
        refund.setReason(request.reason());
        refund.setStatus(RefundStatus.REQUESTED);
        paymentRefundRepository.save(refund);

        createTransaction(
                payment,
                TransactionType.REFUND_REQUESTED,
                TransactionStatus.PENDING,
                request.reason() == null || request.reason().isBlank() ? "Refund requested" : request.reason(),
                refund.getRefundReference(),
                request.amount()
        );

        refund.setStatus(RefundStatus.PROCESSED);
        paymentRefundRepository.save(refund);

        payment.setRefundedAmount(alreadyRefunded.add(request.amount()));
        paymentStateMachine.transition(payment, payment.getRefundedAmount().compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED);
        paymentRepository.save(payment);

        createTransaction(
                payment,
                TransactionType.REFUND_COMPLETED,
                TransactionStatus.SUCCESS,
                request.reason() == null || request.reason().isBlank() ? "Refund completed" : request.reason(),
                refund.getRefundReference(),
                request.amount()
        );

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            orderService.markRefunded(payment.getOrder());
        }

        auditService.record("PAYMENT_REFUNDED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "Refund processed");
        paymentEventPublisher.publish("payment.refunded", payment, Map.of(
                "actor", actor.getEmail(),
                "refundId", refund.getId().toString(),
                "refundReference", refund.getRefundReference(),
                "refundAmount", request.amount().toPlainString(),
                "reason", request.reason() == null ? "" : request.reason()
        ));

        RefundResponse response = toRefundResponse(refund);
        idempotencyService.complete(idempotency.record(), response, refund.getId().toString());
        return response;
    }

    public Page<PaymentResponse> getPayments(User actor, PaymentStatus status, Pageable pageable, boolean adminView) {
        if (adminView) {
            return status == null
                    ? paymentRepository.findAll(pageable).map(this::toResponse)
                    : paymentRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return status == null
                ? paymentRepository.findByOrderUser(actor, pageable).map(this::toResponse)
                : paymentRepository.findByOrderUserAndStatus(actor, status, pageable).map(this::toResponse);
    }

    public PaymentResponse getPayment(UUID paymentId, User actor, boolean adminView) {
        return toResponse(getOwnedPayment(paymentId, actor, adminView));
    }

    private Payment getOwnedPayment(UUID paymentId, User actor, boolean adminView) {
        if (adminView) {
            return paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
        }
        return paymentRepository.findByIdAndOrderUser(paymentId, actor)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found"));
    }

    void createSystemTransaction(Payment payment, TransactionType type, TransactionStatus status, String remarks, String providerReference, java.math.BigDecimal amount) {
        createTransaction(payment, type, status, remarks, providerReference, amount);
    }

    private void createTransaction(Payment payment, TransactionType type, TransactionStatus status, String remarks, String providerReference) {
        createTransaction(payment, type, status, remarks, providerReference, payment.getAmount());
    }

    private void createTransaction(Payment payment, TransactionType type, TransactionStatus status, String remarks, String providerReference, java.math.BigDecimal amount) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(amount);
        transaction.setProviderReference(providerReference);
        transaction.setRemarks(remarks);
        paymentTransactionRepository.save(transaction);
    }

    private TransactionMode resolveMode(TransactionMode requestedMode) {
        return requestedMode == null ? TransactionMode.PRODUCTION : requestedMode;
    }

    private String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toUpperCase();
        return normalized.isBlank() ? SIMULATED_PROVIDER : normalized;
    }

    private PaymentResponse toResponse(Payment payment) {
        List<PaymentTransactionResponse> transactions = paymentTransactionRepository.findByPaymentIdOrderByCreatedAtDesc(payment.getId()).stream()
                .map(transaction -> new PaymentTransactionResponse(
                        transaction.getId(),
                        transaction.getType().name(),
                        transaction.getStatus().name(),
                        transaction.getAmount(),
                        transaction.getProviderReference(),
                        transaction.getRemarks(),
                        transaction.getCreatedAt()
                ))
                .toList();
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getOrder().getOrderReference(),
                payment.getAmount(),
                payment.getRefundedAmount(),
                payment.getCurrency(),
                payment.getProvider(),
                payment.getProviderOrderId(),
                payment.getProviderPaymentId(),
                payment.getMethod().name(),
                payment.getTransactionMode().name(),
                payment.getStatus().name(),
                payment.getCheckoutUrl(),
                payment.isSimulated(),
                payment.getProviderSignature(),
                payment.getNotes(),
                payment.getCreatedAt(),
                transactions
        );
    }

    RefundResponse toRefundResponse(PaymentRefund refund) {
        Payment payment = refund.getPayment();
        return new RefundResponse(
                refund.getId(),
                refund.getRefundReference(),
                payment.getId(),
                payment.getOrder().getOrderReference(),
                refund.getAmount(),
                payment.getRefundedAmount(),
                payment.getStatus().name(),
                refund.getReason(),
                refund.getCreatedAt()
        );
    }
}
