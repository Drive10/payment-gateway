package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Order;
import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.PaymentTransaction;
import dev.payment.paymentservice.domain.User;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.domain.enums.TransactionMode;
import dev.payment.paymentservice.domain.enums.TransactionStatus;
import dev.payment.paymentservice.domain.enums.TransactionType;
import dev.payment.paymentservice.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.dto.response.PaymentResponse;
import dev.payment.paymentservice.dto.response.PaymentTransactionResponse;
import dev.payment.paymentservice.exception.ApiException;
import dev.payment.paymentservice.integration.processor.PaymentProcessorCaptureResponse;
import dev.payment.paymentservice.integration.processor.PaymentProcessorClient;
import dev.payment.paymentservice.integration.processor.PaymentProcessorIntentResponse;
import dev.payment.paymentservice.repository.PaymentRepository;
import dev.payment.paymentservice.repository.PaymentTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String SIMULATED_PROVIDER = "RAZORPAY_SIMULATOR";

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderService orderService;
    private final AuditService auditService;
    private final PaymentProcessorClient paymentProcessorClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            OrderService orderService,
            AuditService auditService,
            PaymentProcessorClient paymentProcessorClient
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.orderService = orderService;
        this.auditService = auditService;
        this.paymentProcessorClient = paymentProcessorClient;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, User actor, boolean adminView) {
        Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return toResponse(existing);
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
        paymentRepository.save(payment);

        createTransaction(
                payment,
                TransactionType.PAYMENT_INITIATED,
                TransactionStatus.PENDING,
                payment.getTransactionMode() == TransactionMode.TEST ? "Test simulator order created" : "Production payment order created",
                payment.getProviderOrderId()
        );
        orderService.markPaymentPending(order);
        auditService.record("PAYMENT_CREATED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "Payment initiated for order " + order.getOrderReference());
        log.info("event=payment_created paymentId={} orderId={} actor={} provider={} mode={} simulated={}",
                payment.getId(), order.getId(), actor.getEmail(), payment.getProvider(), payment.getTransactionMode(), payment.isSimulated());
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse capturePayment(UUID paymentId, CapturePaymentRequest request, User actor, boolean adminView) {
        Payment payment = getOwnedPayment(paymentId, actor, adminView);
        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return toResponse(payment);
        }

        PaymentProcessorCaptureResponse processorCapture = paymentProcessorClient.capture(payment, request, payment.getTransactionMode());
        payment.setProviderPaymentId(processorCapture.providerPaymentId());
        payment.setProviderSignature(processorCapture.providerSignature());
        payment.setSimulated(processorCapture.simulated());
        payment.setStatus(PaymentStatus.CAPTURED);
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
        log.info("event=payment_captured paymentId={} actor={} mode={} simulated={}", payment.getId(), actor.getEmail(), payment.getTransactionMode(), payment.isSimulated());
        return toResponse(payment);
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

    private void createTransaction(Payment payment, TransactionType type, TransactionStatus status, String remarks, String providerReference) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPayment(payment);
        transaction.setType(type);
        transaction.setStatus(status);
        transaction.setAmount(payment.getAmount());
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
}
