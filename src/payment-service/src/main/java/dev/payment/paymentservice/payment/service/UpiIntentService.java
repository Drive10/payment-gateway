package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.PaymentMethod;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.exception.ApiException;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UpiIntentService {

    private static final Logger log = LoggerFactory.getLogger(UpiIntentService.class);
    
    private static final Duration UPI_EXPIRY = Duration.ofMinutes(5);

    @Value("${application.payment.vpa:payflow@upi}")
    private String defaultVpa;

    @Value("${application.payment.merchant-name:PayFlow Merchant}")
    private String merchantName;

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;

    public UpiIntentService(
            PaymentRepository paymentRepository,
            PaymentStateMachine paymentStateMachine) {
        this.paymentRepository = paymentRepository;
        this.paymentStateMachine = paymentStateMachine;
    }

    public record UpiIntentResponse(
            String upiLink,
            String transactionId,
            String status,
            int expiryMinutes,
            Instant expiresAt
    ) {}

    public UpiIntentResponse createUpiIntent(Payment payment, String upiId) {
        if (payment.getMethod() != PaymentMethod.UPI) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD", 
                    "Payment method must be UPI");
        }

        String targetVpa = upiId != null && !upiId.isBlank() ? upiId : defaultVpa;
        
        String upiLink = buildUpiDeepLink(
                targetVpa,
                merchantName,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getProviderOrderId()
        );

        paymentStateMachine.transition(payment, PaymentStatus.AWAITING_UPI_PAYMENT);
        payment.setUpiId(targetVpa);
        payment.setUpiLink(upiLink);
        paymentRepository.save(payment);

        log.info("Created UPI intent for payment {} with VPA {}, expires in {} minutes",
                payment.getId(), targetVpa, UPI_EXPIRY.toMinutes());

        return new UpiIntentResponse(
                upiLink,
                payment.getId().toString(),
                "AWAITING_UPI_PAYMENT",
                (int) UPI_EXPIRY.toMinutes(),
                Instant.now().plus(UPI_EXPIRY)
        );
    }

    public UpiIntentResponse getUpiIntent(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND",
                        "Payment not found: " + paymentId));

        if (payment.getMethod() != PaymentMethod.UPI) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAYMENT_METHOD",
                    "Payment is not a UPI payment");
        }

        return new UpiIntentResponse(
                payment.getUpiLink(),
                payment.getId().toString(),
                payment.getStatus().name(),
                getRemainingMinutes(payment),
                payment.getUpdatedAt().plus(UPI_EXPIRY)
        );
    }

    public boolean isExpired(Payment payment) {
        if (payment.getMethod() != PaymentMethod.UPI) {
            return false;
        }
        return payment.getStatus() == PaymentStatus.AWAITING_UPI_PAYMENT
                && Duration.between(payment.getUpdatedAt(), Instant.now()).compareTo(UPI_EXPIRY) > 0;
    }

    public Optional<Payment> findAndExpireStaleUpiPayments() {
        Instant threshold = Instant.now().minus(UPI_EXPIRY);
        return paymentRepository.findAll().stream()
                .filter(p -> p.getMethod() == PaymentMethod.UPI
                        && p.getStatus() == PaymentStatus.AWAITING_UPI_PAYMENT
                        && p.getUpdatedAt().isBefore(threshold))
                .findFirst()
                .map(p -> {
                    paymentStateMachine.transition(p, PaymentStatus.EXPIRED);
                    p.setNotes("UPI payment expired after " + UPI_EXPIRY.toMinutes() + " minutes");
                    return paymentRepository.save(p);
                });
    }

    private int getRemainingMinutes(Payment payment) {
        long minutesLeft = Duration.between(Instant.now(), 
                payment.getUpdatedAt().plus(UPI_EXPIRY)).toMinutes();
        return Math.max(0, (int) minutesLeft);
    }

    private String buildUpiDeepLink(String vpa, String name, BigDecimal amount, String currency, String reference) {
        StringBuilder sb = new StringBuilder("upi://pay?");
        
        sb.append("pa=").append(URLEncoder.encode(vpa, StandardCharsets.UTF_8));
        sb.append("&pn=").append(URLEncoder.encode(name, StandardCharsets.UTF_8));
        sb.append("&am=").append(amount.setScale(2));
        sb.append("&cu=").append(currency);
        sb.append("&tn=").append(URLEncoder.encode("Payment ref: " + reference, StandardCharsets.UTF_8));
        
        return sb.toString();
    }
}