package dev.payment.paymentservice.service;

import dev.payment.paymentservice.domain.Dispute;
import dev.payment.paymentservice.domain.DisputeEvidence;
import dev.payment.paymentservice.domain.Payment;
import dev.payment.paymentservice.repository.DisputeRepository;
import dev.payment.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);
    private static final int DEFAULT_DUE_DAYS = 7;

    private final DisputeRepository disputeRepository;
    private final PaymentRepository paymentRepository;

    public DisputeService(DisputeRepository disputeRepository, PaymentRepository paymentRepository) {
        this.disputeRepository = disputeRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Dispute createDispute(UUID paymentId, UUID merchantId, String reason, String description,
                                   String customerEmail, String customerName) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (disputeRepository.existsByPaymentId(paymentId)) {
            throw new IllegalStateException("Dispute already exists for this payment");
        }

        Dispute dispute = new Dispute();
        dispute.setPaymentId(paymentId);
        dispute.setMerchantId(merchantId);
        dispute.setAmount(payment.getAmount());
        dispute.setCurrency(payment.getCurrency());
        dispute.setReason(Dispute.DisputeReason.valueOf(reason));
        dispute.setDescription(description);
        dispute.setCustomerEmail(customerEmail);
        dispute.setCustomerName(customerName);
        dispute.setChargebackAmount(payment.getAmount());
        dispute.setChargebackCurrency(payment.getCurrency());
        dispute.setInitiatedBy("MERCHANT");
        dispute.setInitiatedAt(Instant.now());
        dispute.setDueBy(Instant.now().plus(DEFAULT_DUE_DAYS, ChronoUnit.DAYS));
        dispute.setStatus(Dispute.DisputeStatus.OPEN);

        log.info("Dispute created for payment {} by merchant {}: {}", paymentId, merchantId, dispute.getDisputeReference());

        return disputeRepository.save(dispute);
    }

    @Transactional
    public DisputeEvidence submitEvidence(UUID disputeId, String evidenceType, String fileUrl,
                                           String fileKey, String description, String submittedBy) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        if (dispute.getStatus() == Dispute.DisputeStatus.WON ||
            dispute.getStatus() == Dispute.DisputeStatus.LOST ||
            dispute.getStatus() == Dispute.DisputeStatus.CLOSED) {
            throw new IllegalStateException("Cannot submit evidence for closed dispute");
        }

        DisputeEvidence evidence = new DisputeEvidence();
        evidence.setDisputeId(disputeId);
        evidence.setEvidenceType(evidenceType);
        evidence.setFileUrl(fileUrl);
        evidence.setFileKey(fileKey);
        evidence.setDescription(description);
        evidence.setSubmittedBy(submittedBy);
        evidence.setSubmittedAt(Instant.now());

        dispute.setStatus(Dispute.DisputeStatus.UNDER_REVIEW);

        log.info("Evidence submitted for dispute {}", dispute.getDisputeReference());

        return disputeRepository.save(dispute).getStatus() != null ? null : null;
    }

    public List<DisputeEvidence> getDisputeEvidence(UUID disputeId) {
        return List.of();
    }

    @Transactional
    public Dispute acceptDispute(UUID disputeId, String notes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.setStatus(Dispute.DisputeStatus.WON);
        dispute.setWonAt(Instant.now());
        dispute.setClosedAt(Instant.now());

        log.info("Dispute {} accepted (merchant won)", dispute.getDisputeReference());

        return disputeRepository.save(dispute);
    }

    @Transactional
    public Dispute rejectDispute(UUID disputeId, String notes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.setStatus(Dispute.DisputeStatus.LOST);
        dispute.setLostAt(Instant.now());
        dispute.setClosedAt(Instant.now());

        log.info("Dispute {} rejected (merchant lost)", dispute.getDisputeReference());

        return disputeRepository.save(dispute);
    }

    @Transactional
    public Dispute escalateDispute(UUID disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + disputeId));

        dispute.setStatus(Dispute.DisputeStatus.ESCALATED);

        log.info("Dispute {} escalated", dispute.getDisputeReference());

        return disputeRepository.save(dispute);
    }

    public Page<Dispute> getMerchantDisputes(UUID merchantId, Dispute.DisputeStatus status, Pageable pageable) {
        if (merchantId == null) {
            return status != null
                    ? disputeRepository.findByStatus(status, pageable)
                    : disputeRepository.findAll(pageable);
        }
        return status != null
                ? disputeRepository.findByMerchantIdAndStatus(merchantId, status, pageable)
                : disputeRepository.findByMerchantId(merchantId, pageable);
    }

    public Page<Dispute> getAllDisputes(Dispute.DisputeStatus status, Pageable pageable) {
        return status != null
                ? disputeRepository.findByStatus(status, pageable)
                : disputeRepository.findAll(pageable);
    }

    public Optional<Dispute> getDispute(UUID disputeId) {
        return disputeRepository.findById(disputeId);
    }

    public Optional<Dispute> getDisputeByReference(String reference) {
        return disputeRepository.findByDisputeReference(reference);
    }

    public List<Dispute> getPaymentDisputes(UUID paymentId) {
        return disputeRepository.findByPaymentId(paymentId);
    }

    public long countOpenDisputes(UUID merchantId) {
        return disputeRepository.countByMerchantIdAndStatus(merchantId, Dispute.DisputeStatus.OPEN);
    }

    public BigDecimal getDisputedAmount(UUID merchantId) {
        return disputeRepository.findByMerchantIdAndStatus(merchantId, Dispute.DisputeStatus.OPEN, Pageable.unpaged())
                .getContent()
                .stream()
                .map(Dispute::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
