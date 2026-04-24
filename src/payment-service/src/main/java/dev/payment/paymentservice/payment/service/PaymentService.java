package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Order;
import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.PaymentRefund;
import dev.payment.paymentservice.payment.domain.User;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.domain.enums.OrderStatus;
import dev.payment.paymentservice.payment.domain.enums.RefundStatus;
import dev.payment.paymentservice.payment.domain.enums.TransactionStatus;
import dev.payment.paymentservice.payment.domain.enums.TransactionMode;
import dev.payment.paymentservice.payment.domain.enums.TransactionType;
import dev.payment.paymentservice.payment.dto.FeeCalculation;
import dev.payment.paymentservice.payment.dto.request.CapturePaymentRequest;
import dev.payment.paymentservice.payment.dto.request.CreatePaymentRequest;
import dev.payment.paymentservice.payment.dto.request.CreateRefundRequest;
import dev.payment.paymentservice.payment.dto.response.PaymentResponse;
import dev.payment.paymentservice.payment.dto.response.CardTokenizationResponse;
import dev.payment.paymentservice.payment.dto.response.RefundResponse;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.integration.client.OrderServiceClient;
import dev.payment.paymentservice.payment.integration.processor.PaymentProcessorCaptureResponse;
import dev.payment.paymentservice.payment.integration.processor.PaymentProcessorClient;
import dev.payment.paymentservice.payment.integration.processor.PaymentProcessorIntentResponse;
import dev.payment.paymentservice.payment.mapper.PaymentMapper;
import dev.payment.paymentservice.payment.repository.PaymentRefundRepository;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import dev.payment.paymentservice.payment.service.bin.CardBinService;
import dev.payment.paymentservice.payment.service.bin.CardBinService.CardBinData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String SIMULATED_PROVIDER = "RAZORPAY_SIMULATOR";

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final OrderServiceClient orderServiceClient;
    private final AuditService auditService;
    private final PaymentProcessorClient paymentProcessorClient;
    private final PaymentEventPublisher paymentEventPublisher;
    private final IdempotencyService idempotencyService;
    private final PaymentStateMachine paymentStateMachine;
    private final FeeEngine feeEngine;
    private final LedgerService ledgerService;
    private final PaymentTransactionService transactionService;
    private final PaymentMapper paymentMapper;
    private final CardBinService cardBinService;
    private final PaymentValidator paymentValidator;
    private final UpiIntentService upiIntentService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository paymentRefundRepository,
            OrderServiceClient orderServiceClient,
            AuditService auditService,
            PaymentProcessorClient paymentProcessorClient,
            PaymentEventPublisher paymentEventPublisher,
            IdempotencyService idempotencyService,
            PaymentStateMachine paymentStateMachine,
            FeeEngine feeEngine,
            LedgerService ledgerService,
            PaymentTransactionService transactionService,
            PaymentMapper paymentMapper,
            CardBinService cardBinService,
            PaymentValidator paymentValidator,
            UpiIntentService upiIntentService) {
        this.paymentRepository = paymentRepository;
        this.paymentRefundRepository = paymentRefundRepository;
        this.orderServiceClient = orderServiceClient;
        this.auditService = auditService;
        this.paymentProcessorClient = paymentProcessorClient;
        this.paymentEventPublisher = paymentEventPublisher;
        this.idempotencyService = idempotencyService;
        this.paymentStateMachine = paymentStateMachine;
        this.feeEngine = feeEngine;
        this.ledgerService = ledgerService;
        this.transactionService = transactionService;
        this.paymentMapper = paymentMapper;
        this.cardBinService = cardBinService;
        this.paymentValidator = paymentValidator;
        this.upiIntentService = upiIntentService;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, String idempotencyKey, User actor) {
        paymentValidator.validateOrThrow(request);
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
        payment.setStatus(PaymentStatus.PENDING);
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
            Order order = createLocalOrder(request);
            
            // Check for duplicate payment - prevent same order from being paid twice
            EnumSet<PaymentStatus> activeStatuses = EnumSet.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.CREATED,
                    PaymentStatus.AWAITING_UPI_PAYMENT,
                    PaymentStatus.AUTHORIZATION_PENDING,
                    PaymentStatus.AUTHORIZED,
                    PaymentStatus.PROCESSING
            );
            
            if (paymentRepository.existsByOrderIdAndStatusIn(order.getId(), activeStatuses)) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_PAYMENT",
                        "A payment for this order is already in progress or completed. Order ID: " + order.getId()
                );
            }
            
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
            paymentStateMachine.transition(payment, PaymentStatus.CREATED);

            Payment savedPayment = savePayment(payment, idempotencyKey);
            applyPostCreationLifecycle(savedPayment);
            return paymentRepository.save(savedPayment);
        } catch (DataIntegrityViolationException exception) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
        }
    }

    private Order createLocalOrder(CreatePaymentRequest request) {
        Order order = new Order();
        order.setId(request.orderId() != null ? request.orderId() : UUID.randomUUID());
        order.setOrderReference("ORD-" + order.getId().toString().substring(0, 8).toUpperCase());
        
        // Try to get amount from order snapshot if available
        if (request.order() != null) {
            order.setAmount(request.order().amount());
            order.setCurrency(request.order().currency());
        } else {
            // Default values - will be set from payment later
            order.setAmount(BigDecimal.valueOf(50000));
            order.setCurrency("INR");
        }
        
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        return order;
    }

    private void applyPostCreationLifecycle(Payment payment) {
        if (payment.getMethod() == PaymentMethod.UPI) {
            String upiId = extractUpiIdFromNotes(payment.getNotes());
            UpiIntentService.UpiIntentResponse upiIntent = upiIntentService.createUpiIntent(payment, upiId);
            payment.setCheckoutUrl(upiIntent.upiLink());
            transactionService.createTransaction(
                    payment,
                    TransactionType.PAYMENT,
                    TransactionStatus.PENDING,
                    "UPI collect request initiated",
                    payment.getProviderOrderId(),
                    payment.getAmount()
            );
            paymentEventPublisher.publish("payment.upi.collect.created", payment, Map.of(
                    "upiId", payment.getUpiId() != null ? payment.getUpiId() : "",
                    "expiresAt", upiIntent.expiresAt().toString()
            ));
            return;
        }

        if (payment.getMethod() == PaymentMethod.CARD) {
            // For card flows we model authorization as a separate state before capture.
            paymentStateMachine.transition(payment, PaymentStatus.AUTHORIZED);
            transactionService.createTransaction(
                    payment,
                    TransactionType.PAYMENT,
                    TransactionStatus.SUCCESS,
                    "Payment authorized and ready for capture",
                    payment.getProviderOrderId(),
                    payment.getAmount()
            );
            paymentEventPublisher.publish("payment.authorized", payment, Map.of());
        }
    }

    private String extractUpiIdFromNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String prefix = "UPI_ID=";
        for (String token : notes.split("\\|")) {
            String trimmed = token.trim();
            if (trimmed.startsWith(prefix)) {
                String upiId = trimmed.substring(prefix.length()).trim();
                return upiId.isBlank() ? null : upiId;
            }
        }
        return null;
    }

    private Payment savePayment(Payment payment, String idempotencyKey) {
        Payment saved = paymentRepository.save(payment);
        transactionService.createPaymentInitiated(saved);
        orderServiceClient.updateOrderStatus(payment.getOrder().getOrderReference(), "PAYMENT_PENDING");
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

    @Transactional
    public PaymentResponse verifyOtp(UUID paymentId, String otp, User actor) {
        Payment payment = getOwnedPayment(paymentId, actor);
        
        if (payment.getStatus() != PaymentStatus.PROCESSING && payment.getStatus() != PaymentStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_STATE", "Payment is not in pending state");
        }

        boolean valid = paymentProcessorClient.verifyOtp(payment, otp);
        if (!valid) {
            paymentStateMachine.transition(payment, PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid OTP - please verify with your bank");
        }

        payment.setProviderPaymentId("pay_" + System.currentTimeMillis());
        paymentStateMachine.transition(payment, PaymentStatus.CAPTURED);
        paymentRepository.save(payment);
        
        recordToLedger(payment);
        transactionService.createCaptureSuccess(payment, payment.getProviderPaymentId());
        updateOrderStatus(payment);
        auditService.record("PAYMENT_OTP_VERIFIED", actor.getEmail(), "PAYMENT", payment.getId().toString(), "OTP verified and payment captured");
        paymentEventPublisher.publish("payment.captured", payment, Map.of("actor", actor.getEmail()));
        
        log.info("event=payment_otp_verified paymentId={} actor={}", paymentId, actor.getEmail());
        return paymentMapper.toResponse(payment, transactionService.findByPaymentId(paymentId));
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
            orderServiceClient.updateOrderStatus(getOrderReference(payment), "REFUNDED");
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
            orderServiceClient.updateOrderStatus(getOrderReference(payment), "PAID");
        } catch (Exception orderException) {
            log.warn("event=order_status_update_failed paymentId={} error={}", payment.getId(), orderException.getMessage());
        }
    }

    private void updateOrderStatusOnFailure(Payment payment) {
        try {
            orderServiceClient.updateOrderStatus(getOrderReference(payment), "FAILED");
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
        if (normalized.equals("RAZORPAY_SIMULATOR") || normalized.equals("STRIPE_SIMULATOR") || normalized.equals("PAYPAL_SIMULATOR")) {
            return SIMULATED_PROVIDER;
        }
        return normalized;
    }

private String resolveSimulationMode(Payment payment) {
        if (payment.getTransactionMode() == TransactionMode.TEST) {
            return "TEST";
        }
        return "SUCCESS";
    }

    public CardTokenizationResponse tokenizeCard(dev.payment.paymentservice.payment.dto.request.CardTokenizationRequest request) {
        String cardNumber = request.cardNumber().replaceAll("\\s", "");
        
        if (cardNumber.length() < 13 || cardNumber.length() > 19) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CARD", "Invalid card number");
        }
        
        CardBinService.CardBinData binData = cardBinService.getCardData(cardNumber);
        
        String token = "tok_" + cardNumber.substring(cardNumber.length() - 4) + "_" + System.currentTimeMillis();
        
        log.info("Card tokenized: {}****{} brand={} bank={}", 
            token.substring(0, 8), 
            token.substring(token.length() - 4),
            binData.brand(),
            binData.bankName());
        
        return new CardTokenizationResponse(token, binData);
    }

    @Transactional
    public PaymentResponse retryPayment(UUID paymentId, String idempotencyKey, User actor) {
        Payment payment = getOwnedPayment(paymentId, actor);
        
        if (payment.getStatus() != PaymentStatus.FAILED && payment.getStatus() != PaymentStatus.EXPIRED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE", 
                "Only failed or expired payments can be retried. Current state: " + payment.getStatus());
        }
        
        String newIdempotencyKey = idempotencyKey + "_retry_" + System.currentTimeMillis();
        
        IdempotencyService.IdempotencyResult<PaymentResponse> idempotency =
                idempotencyService.begin("PAYMENT_RETRY", newIdempotencyKey, actorIdKey(actor), Map.of(
                        "actor", actor.getEmail(),
                        "originalPaymentId", paymentId
                ), PaymentResponse.class);
        if (idempotency.replayed()) {
            return idempotency.cachedResponse();
        }
        
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProviderOrderId(null);
        payment.setProviderPaymentId(null);
        payment.setProviderSignature(null);
        payment.setCheckoutUrl(null);
        payment.setIdempotencyKey(newIdempotencyKey);
        
        try {
            PaymentProcessorIntentResponse processorIntent = paymentProcessorClient.createIntent(
                    payment, payment.getOrder().getOrderReference(), payment.getTransactionMode());
            payment.setProviderOrderId(processorIntent.providerOrderId());
            payment.setCheckoutUrl(processorIntent.checkoutUrl());
            paymentStateMachine.transition(payment, PaymentStatus.CREATED);
        } catch (Exception e) {
            paymentStateMachine.transition(payment, PaymentStatus.FAILED);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "RETRY_FAILED", "Failed to initiate retry: " + e.getMessage());
        }
        
        paymentRepository.save(payment);
        transactionService.createPaymentInitiated(payment);
        
        PaymentResponse response = paymentMapper.toResponse(payment, transactionService.findByPaymentId(payment.getId()));
        idempotencyService.complete(idempotency.record(), response, payment.getId().toString());
        
        log.info("event=payment_retry paymentId={} actor={}", paymentId, actor.getEmail());
        return response;
    }
}
