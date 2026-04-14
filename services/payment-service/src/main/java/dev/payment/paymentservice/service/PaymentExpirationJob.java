package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.domain.enums.PaymentStatus;
import dev.payment.paymentservice.repository.PaymentRepository;
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
 */
@Component
@Profile("!test")
public class PaymentExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpirationJob.class);
    
    // Payments in these states can expire
    private static final EnumSet<PaymentStatus> EXPIRABLE_STATUSES = EnumSet.of(
            PaymentStatus.CREATED,
            PaymentStatus.AUTHORIZATION_PENDING,
            PaymentStatus.AUTHORIZED
    );

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;
    private final boolean enabled;
    private final Duration expirationTime;

    public PaymentExpirationJob(
            PaymentRepository paymentRepository,
            PaymentStateMachine stateMachine,
            @Value("${application.expiration.enabled:true}") boolean enabled,
            @Value("${application.expiration.timeout:PT15M}") Duration expirationTime
    ) {
        this.paymentRepository = paymentRepository;
        this.stateMachine = stateMachine;
        this.enabled = enabled;
        this.expirationTime = expirationTime;
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void expirePendingPayments() {
        if (!enabled) {
            return;
        }

        Instant expirationThreshold = Instant.now().minus(expirationTime);
        
        // Use existing method with updatedAt - returns List
        List<Payment> expiredPayments = paymentRepository
                .findByStatusInAndUpdatedAtAfterOrderByUpdatedAtAsc(
                        EXPIRABLE_STATUSES, 
                        expirationThreshold, 
                        PageRequest.of(0, 100)
                );

        if (expiredPayments.isEmpty()) {
            return;
        }

        log.info("Found {} payments to expire", expiredPayments.size());
        
        for (Payment payment : expiredPayments) {
            try {
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
}