package dev.payment.paymentservice.payment.service;

import dev.payment.paymentservice.payment.domain.Payment;
import dev.payment.paymentservice.payment.domain.enums.PaymentStatus;
import dev.payment.paymentservice.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

/**
 * Expiration job - fails payments that have been in AUTHORIZATION_PENDING too long.
 * This simulates real-world timeout where bank doesn't respond.
 * 
 * UPI payments have a shorter timeout (5 min default) as UPI transactions 
 * are expected to complete quickly.
 */
@Component
@Profile("!test")
public class PaymentExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationJob.class);
    
    // Payments in these states can expire (card/bank flows)
    private static final EnumSet<PaymentStatus> EXPIRABLE_STATUSES = EnumSet.of(
            PaymentStatus.CREATED,
            PaymentStatus.AUTHORIZATION_PENDING,
            PaymentStatus.AUTHORIZED
    );

    // UPI-specific expirable states - shorter timeout
    private static final EnumSet<PaymentStatus> UPI_EXPIRABLE_STATUSES = EnumSet.of(
            PaymentStatus.AWAITING_UPI_PAYMENT
    );

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final UpiIntentService upiIntentService;
    private final boolean enabled;
    private final Duration expirationTime;
    private final Duration upiExpirationTime;

    public PaymentExpirationJob(
            PaymentRepository paymentRepository,
            PaymentStateMachine stateMachine,
            UpiIntentService upiIntentService,
            @Value("${application.expiration.enabled:true}") boolean enabled,
            @Value("${application.expiration.timeout:PT15M}") Duration expirationTime,
            @Value("${application.expiration.upi-timeout:PT5M}") Duration upiExpirationTime
    ) {
        this.paymentRepository = paymentRepository;
        this.stateMachine = stateMachine;
        this.upiIntentService = upiIntentService;
        this.enabled = enabled;
        this.expirationTime = expirationTime;
        this.upiExpirationTime = upiExpirationTime;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void expirePendingPayments() {
        if (!enabled) {
            return;
        }

        // Expire standard payments (card, netbanking, etc.)
        expireStandardPayments();
        
        // Expire UPI payments (shorter timeout)
        expireUpiPayments();
    }

    private void expireStandardPayments() {
        Instant expirationThreshold = Instant.now().minus(expirationTime);
        
        List<Payment> expiredPayments = paymentRepository
                .findByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                        EXPIRABLE_STATUSES, 
                        expirationThreshold, 
                        PageRequest.of(0, 100)
                );

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Found {} standard payments to expire", expiredPayments.size());
        
        for (Payment payment : expiredPayments) {
            try {
                // Skip UPI payments - handled separately
                if (payment.getStatus() == PaymentStatus.AWAITING_UPI_PAYMENT) {
                    continue;
                }
                
                stateMachine.transition(payment, PaymentStatus.EXPIRED);
                payment.setNotes("Expired due to timeout - no response from payment provider");
                paymentRepository.save(payment);
                
                log.info("Expired payment {} - was in {} state for > {} minutes",
                        payment.getId(), 
                        payment.getStatus() != null ? payment.getStatus() : "UNKNOWN",
                        expirationTime.toMinutes());
                        
            } catch (Exception e) {
                log.error("Failed to expire payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }

    private void expireUpiPayments() {
        Instant upiExpirationThreshold = Instant.now().minus(upiExpirationTime);
        
        List<Payment> upiPayments = paymentRepository
                .findByStatusInAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                        UPI_EXPIRABLE_STATUSES, 
                        upiExpirationThreshold, 
                        PageRequest.of(0, 100)
                );

        if (upiPayments.isEmpty()) {
            return;
        }

        log.info("Found {} UPI payments to expire", upiPayments.size());
        
        for (Payment payment : upiPayments) {
            try {
                stateMachine.transition(payment, PaymentStatus.EXPIRED);
                payment.setNotes("UPI payment expired - user did not complete payment within " 
                        + upiExpirationTime.toMinutes() + " minutes");
                paymentRepository.save(payment);
                
                log.info("Expired UPI payment {} - user did not complete payment within {} minutes",
                        payment.getId(), 
                        upiExpirationTime.toMinutes());
                        
            } catch (Exception e) {
                log.error("Failed to expire UPI payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }
}
