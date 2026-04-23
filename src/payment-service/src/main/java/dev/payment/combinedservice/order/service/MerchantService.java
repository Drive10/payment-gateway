package dev.payment.combinedservice.order.service;

import dev.payment.combinedservice.order.entity.BankAccount;
import dev.payment.combinedservice.order.entity.KycDocument;
import dev.payment.combinedservice.order.entity.Merchant;
import dev.payment.combinedservice.order.repository.BankAccountRepository;
import dev.payment.combinedservice.order.repository.KycDocumentRepository;
import dev.payment.combinedservice.order.repository.MerchantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MerchantService {

    private static final Logger log = LoggerFactory.getLogger(MerchantService.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final MerchantRepository merchantRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final BankAccountRepository bankAccountRepository;

    public MerchantService(
            MerchantRepository merchantRepository,
            KycDocumentRepository kycDocumentRepository,
            BankAccountRepository bankAccountRepository) {
        this.merchantRepository = merchantRepository;
        this.kycDocumentRepository = kycDocumentRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public Merchant createMerchant(Merchant merchant) {
        if (merchantRepository.existsByEmail(merchant.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + merchant.getEmail());
        }

        merchant.setApiKey(generateApiKey());
        merchant.setWebhookSecret(generateWebhookSecret());
        merchant.setKycStatus("PENDING");
        merchant.setStatus("ACTIVE");

        log.info("Creating merchant: {} ({})", merchant.getBusinessName(), merchant.getEmail());
        return merchantRepository.save(merchant);
    }

    public Optional<Merchant> getMerchant(UUID id) {
        return merchantRepository.findById(id);
    }

    public Optional<Merchant> getMerchantByEmail(String email) {
        return merchantRepository.findByEmail(email);
    }

    public Optional<Merchant> getMerchantByApiKey(String apiKey) {
        return merchantRepository.findByApiKey(apiKey);
    }

    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    public List<Merchant> getMerchantsByStatus(String status) {
        return merchantRepository.findByStatus(status);
    }

    public List<Merchant> getMerchantsByKycStatus(String kycStatus) {
        return merchantRepository.findByKycStatus(kycStatus);
    }

    public Merchant updateMerchant(UUID id, Merchant updates) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + id));

        if (updates.getBusinessName() != null) {
            merchant.setBusinessName(updates.getBusinessName());
        }
        if (updates.getLegalName() != null) {
            merchant.setLegalName(updates.getLegalName());
        }
        if (updates.getPhone() != null) {
            merchant.setPhone(updates.getPhone());
        }
        if (updates.getWebsite() != null) {
            merchant.setWebsite(updates.getWebsite());
        }
        if (updates.getBusinessType() != null) {
            merchant.setBusinessType(updates.getBusinessType());
        }
        if (updates.getWebhookUrl() != null) {
            merchant.setWebhookUrl(updates.getWebhookUrl());
        }

        return merchantRepository.save(merchant);
    }

    public Merchant updateKycStatus(UUID id, String status, String verifiedBy, String notes) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + id));

        merchant.setKycStatus(status);
        merchant.setVerificationNotes(notes);

        if ("VERIFIED".equals(status)) {
            merchant.setKycVerifiedAt(Instant.now());
            merchant.setKycVerifiedBy(verifiedBy);
        }

        log.info("KYC status updated for merchant {}: {} -> {}", id, merchant.getKycStatus(), status);
        return merchantRepository.save(merchant);
    }

    public Merchant suspendMerchant(UUID id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + id));

        merchant.setStatus("SUSPENDED");
        log.info("Merchant suspended: {}", id);
        return merchantRepository.save(merchant);
    }

    public Merchant activateMerchant(UUID id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + id));

        merchant.setStatus("ACTIVE");
        log.info("Merchant activated: {}", id);
        return merchantRepository.save(merchant);
    }

    // KYC Document operations
    public KycDocument addKycDocument(UUID merchantId, KycDocument document) {
        document.setMerchantId(merchantId);
        document.setStatus("PENDING");
        log.info("KYC document added for merchant {}: {}", merchantId, document.getDocumentType());
        return kycDocumentRepository.save(document);
    }

    public List<KycDocument> getKycDocuments(UUID merchantId) {
        return kycDocumentRepository.findByMerchantId(merchantId);
    }

    public KycDocument verifyKycDocument(UUID documentId, String verifiedBy) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.setStatus("VERIFIED");
        document.setVerifiedAt(Instant.now());
        document.setVerifiedBy(verifiedBy);

        return kycDocumentRepository.save(document);
    }

    public KycDocument rejectKycDocument(UUID documentId, String reason) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.setStatus("REJECTED");
        document.setRejectionReason(reason);

        return kycDocumentRepository.save(document);
    }

    // Bank Account operations
    public BankAccount addBankAccount(UUID merchantId, BankAccount bankAccount) {
        if (bankAccount.getIsDefault() != null && bankAccount.getIsDefault()) {
            clearDefaultBankAccount(merchantId);
        }
        bankAccount.setMerchantId(merchantId);
        log.info("Bank account added for merchant {}: {}", merchantId, bankAccount.getBankName());
        return bankAccountRepository.save(bankAccount);
    }

    public List<BankAccount> getBankAccounts(UUID merchantId) {
        return bankAccountRepository.findByMerchantId(merchantId);
    }

    public BankAccount getDefaultBankAccount(UUID merchantId) {
        return bankAccountRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
                .orElse(null);
    }

    public void setDefaultBankAccount(UUID merchantId, UUID bankAccountId) {
        clearDefaultBankAccount(merchantId);

        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + bankAccountId));

        if (!bankAccount.getMerchantId().equals(merchantId)) {
            throw new IllegalArgumentException("Bank account does not belong to merchant");
        }

        bankAccount.setIsDefault(true);
        bankAccountRepository.save(bankAccount);
    }

    private void clearDefaultBankAccount(UUID merchantId) {
        bankAccountRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
                .ifPresent(account -> {
                    account.setIsDefault(false);
                    bankAccountRepository.save(account);
                });
    }

    public void regenerateApiKey(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + merchantId));

        merchant.setApiKey(generateApiKey());
        merchantRepository.save(merchant);
        log.info("API key regenerated for merchant: {}", merchantId);
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "pk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateWebhookSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
