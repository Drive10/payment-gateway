package dev.payment.analyticsservice.service;

import dev.payment.analyticsservice.entity.Dispute;
import dev.payment.analyticsservice.repository.DisputeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class DisputeService {

    private static final Logger log = LoggerFactory.getLogger(DisputeService.class);
    private static final SecureRandom random = new SecureRandom();

    private final DisputeRepository disputeRepository;

    public DisputeService(DisputeRepository disputeRepository) {
        this.disputeRepository = disputeRepository;
    }

    public Dispute createDispute(Dispute dispute) {
        dispute.setDisputeId(generateDisputeId());
        dispute.setStatus("OPEN");
        dispute.setEvidenceDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        
        log.info("Creating dispute: {} for payment: {}", dispute.getDisputeId(), "REDACTED");
        return disputeRepository.save(dispute);
    }

    public Optional<Dispute> getDispute(UUID id) {
        return disputeRepository.findById(id);
    }

    public Optional<Dispute> getDisputeByDisputeId(String disputeId) {
        return disputeRepository.findByDisputeId(disputeId);
    }

    public Optional<Dispute> getDisputeByPaymentId(String paymentId) {
        return disputeRepository.findByPaymentId(paymentId);
    }

    public List<Dispute> getDisputesByMerchant(UUID merchantId) {
        return disputeRepository.findByMerchantId(merchantId);
    }

    public List<Dispute> getDisputesByMerchantAndStatus(UUID merchantId, String status) {
        return disputeRepository.findByMerchantIdAndStatus(merchantId, status);
    }

    public List<Dispute> getAllDisputes() {
        return disputeRepository.findAll();
    }

    public List<Dispute> getDisputesByStatus(String status) {
        return disputeRepository.findByStatus(status);
    }

    public Dispute updateDisputeStatus(UUID id, String status, String notes, String resolvedBy) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + id));

        dispute.setStatus(status);
        
        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            dispute.setResolvedAt(Instant.now());
            dispute.setResolvedBy(resolvedBy);
            dispute.setResolutionNotes(notes);
        }

        log.info("Dispute {} status updated to: {}", dispute.getDisputeId(), status);
        return disputeRepository.save(dispute);
    }

    public Dispute acceptDispute(UUID id, String resolvedBy) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + id));

        dispute.setStatus("LOST");
        dispute.setResolvedAt(Instant.now());
        dispute.setResolvedBy(resolvedBy);
        dispute.setResolution("ACCEPTED");

        log.info("Dispute {} accepted (lost)", dispute.getDisputeId());
        return disputeRepository.save(dispute);
    }

    public Dispute contestDispute(UUID id, String evidenceNotes, String resolvedBy) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + id));

        dispute.setStatus("UNDER_REVIEW");
        dispute.setResolutionNotes(evidenceNotes);

        log.info("Dispute {} contested", dispute.getDisputeId());
        return disputeRepository.save(dispute);
    }

    public Dispute winDispute(UUID id, String resolvedBy) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + id));

        dispute.setStatus("WON");
        dispute.setResolvedAt(Instant.now());
        dispute.setResolvedBy(resolvedBy);
        dispute.setResolution("WON");
        dispute.setFee(java.math.BigDecimal.ZERO);

        log.info("Dispute {} won", dispute.getDisputeId());
        return disputeRepository.save(dispute);
    }

    public Dispute assignDispute(UUID id, String assignedTo) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found: " + id));

        dispute.setAssignedTo(assignedTo);
        log.info("Dispute {} assigned to: {}", dispute.getDisputeId(), "REDACTED");
        return disputeRepository.save(dispute);
    }

    public List<Dispute> getOverdueDisputes() {
        Instant now = Instant.now();
        return disputeRepository.findByStatus("OPEN").stream()
                .filter(d -> d.getEvidenceDeadline() != null && d.getEvidenceDeadline().isBefore(now))
                .toList();
    }

    private String generateDisputeId() {
        int num = 100000 + random.nextInt(900000);
        return "DSP" + num;
    }
}
