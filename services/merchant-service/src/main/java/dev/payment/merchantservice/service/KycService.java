package dev.payment.merchantservice.service;

import dev.payment.merchantservice.entity.KycDocument;
import dev.payment.merchantservice.entity.KycStatus;
import dev.payment.merchantservice.entity.Merchant;
import dev.payment.merchantservice.repository.KycDocumentRepository;
import dev.payment.merchantservice.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class KycService {

    private static final Logger log = LoggerFactory.getLogger(KycService.class);

    private static final Set<String> REQUIRED_DOCUMENT_TYPES = Set.of(
            "PAN_CARD",
            "AADHAR_CARD",
            "GST_CERTIFICATE"
    );

    private static final Set<String> BUSINESS_DOCUMENT_TYPES = Set.of(
            "BUSINESS_REGISTRATION",
            "PARTNERSHIP_DEED",
            "MOA_AOA"
    );

    private final MerchantRepository merchantRepository;
    private final KycDocumentRepository kycDocumentRepository;

    public KycService(MerchantRepository merchantRepository, KycDocumentRepository kycDocumentRepository) {
        this.merchantRepository = merchantRepository;
        this.kycDocumentRepository = kycDocumentRepository;
    }

    public KycStatus getKycStatus(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .map(m -> KycStatus.valueOf(m.getKycStatus()))
                .orElse(KycStatus.PENDING);
    }

    public boolean canAcceptPayments(UUID merchantId) {
        return getKycStatus(merchantId).canAcceptPayments();
    }

    @Transactional
    public Merchant submitKyc(UUID merchantId, List<KycDocument> documents) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        KycStatus currentStatus = KycStatus.valueOf(merchant.getKycStatus());
        if (!currentStatus.canTransitionTo(KycStatus.SUBMITTED)) {
            throw new IllegalStateException("Cannot submit KYC from status: " + currentStatus);
        }

        validateDocuments(documents);

        for (KycDocument doc : documents) {
            doc.setMerchantId(merchantId);
            doc.setStatus("PENDING");
            kycDocumentRepository.save(doc);
        }

        merchant.setKycStatus(KycStatus.SUBMITTED.name());
        log.info("KYC submitted for merchant {} with {} documents", merchantId, documents.size());

        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant startReview(UUID merchantId, String reviewer) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        KycStatus currentStatus = KycStatus.valueOf(merchant.getKycStatus());
        if (!currentStatus.canTransitionTo(KycStatus.IN_REVIEW)) {
            throw new IllegalStateException("Cannot start review from status: " + currentStatus);
        }

        merchant.setKycStatus(KycStatus.IN_REVIEW.name());
        log.info("KYC review started for merchant {} by {}", merchantId, reviewer);

        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant approveKyc(UUID merchantId, String approvedBy, String notes) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        KycStatus currentStatus = KycStatus.valueOf(merchant.getKycStatus());
        if (!currentStatus.canTransitionTo(KycStatus.VERIFIED)) {
            throw new IllegalStateException("Cannot approve KYC from status: " + currentStatus);
        }

        List<KycDocument> documents = kycDocumentRepository.findByMerchantId(merchantId);
        for (KycDocument doc : documents) {
            if ("PENDING".equals(doc.getStatus())) {
                doc.setStatus("VERIFIED");
                doc.setVerifiedAt(Instant.now());
                doc.setVerifiedBy(approvedBy);
                kycDocumentRepository.save(doc);
            }
        }

        merchant.setKycStatus(KycStatus.VERIFIED.name());
        merchant.setKycVerifiedAt(Instant.now());
        merchant.setKycVerifiedBy(approvedBy);
        merchant.setVerificationNotes(notes);

        log.info("KYC approved for merchant {} by {}", merchantId, approvedBy);

        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant rejectKyc(UUID merchantId, String rejectedBy, String reason) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        KycStatus currentStatus = KycStatus.valueOf(merchant.getKycStatus());
        if (!currentStatus.canTransitionTo(KycStatus.REJECTED)) {
            throw new IllegalStateException("Cannot reject KYC from status: " + currentStatus);
        }

        List<KycDocument> documents = kycDocumentRepository.findByMerchantId(merchantId);
        for (KycDocument doc : documents) {
            if ("PENDING".equals(doc.getStatus())) {
                doc.setStatus("REJECTED");
                doc.setRejectionReason(reason);
                kycDocumentRepository.save(doc);
            }
        }

        merchant.setKycStatus(KycStatus.REJECTED.name());
        merchant.setKycVerifiedBy(rejectedBy);
        merchant.setVerificationNotes(reason);

        log.info("KYC rejected for merchant {} by reason: {}", merchantId, reason);

        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant expireKyc(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        KycStatus currentStatus = KycStatus.valueOf(merchant.getKycStatus());
        if (!currentStatus.canTransitionTo(KycStatus.EXPIRED)) {
            throw new IllegalStateException("Cannot expire KYC from status: " + currentStatus);
        }

        merchant.setKycStatus(KycStatus.EXPIRED.name());
        log.info("KYC expired for merchant {}", merchantId);

        return merchantRepository.save(merchant);
    }

    private void validateDocuments(List<KycDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("At least one KYC document is required");
        }

        long uniqueTypes = documents.stream()
                .map(KycDocument::getDocumentType)
                .distinct()
                .count();

        if (uniqueTypes != documents.size()) {
            throw new IllegalArgumentException("Duplicate document types submitted");
        }

        boolean hasRequiredDocs = documents.stream()
                .map(KycDocument::getDocumentType)
                .anyMatch(REQUIRED_DOCUMENT_TYPES::contains);

        if (!hasRequiredDocs) {
            throw new IllegalArgumentException("At least one required document (PAN, AADHAR, or GST) must be submitted");
        }

        for (KycDocument doc : documents) {
            if (doc.getDocumentType() == null || doc.getDocumentType().isBlank()) {
                throw new IllegalArgumentException("Document type is required");
            }
        }
    }
}
