package dev.payment.combinedservice.order.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.combinedservice.order.dto.*;
import dev.payment.combinedservice.order.entity.ApiKey;
import dev.payment.combinedservice.order.entity.BankAccount;
import dev.payment.combinedservice.order.entity.KycDocument;
import dev.payment.combinedservice.order.entity.KycStatus;
import dev.payment.combinedservice.order.entity.Merchant;
import dev.payment.combinedservice.order.service.ApiKeyService;
import dev.payment.combinedservice.order.service.KycService;
import dev.payment.combinedservice.order.service.MerchantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants")
public class MerchantController {

    private final MerchantService merchantService;
    private final KycService kycService;
    private final ApiKeyService apiKeyService;

    public MerchantController(MerchantService merchantService, KycService kycService, ApiKeyService apiKeyService) {
        this.merchantService = merchantService;
        this.kycService = kycService;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Merchant>> createMerchant(@Valid @RequestBody CreateMerchantRequest request) {
        Merchant merchant = mapToMerchant(request);
        Merchant created = merchantService.createMerchant(merchant);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Merchant>> getMerchant(@PathVariable UUID id) {
        return merchantService.getMerchant(id)
                .map(m -> ResponseEntity.ok(ApiResponse.success(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<ApiResponse<Merchant>> getMerchantByEmail(@PathVariable String email) {
        return merchantService.getMerchantByEmail(email)
                .map(m -> ResponseEntity.ok(ApiResponse.success(m)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Merchant>>> getAllMerchants(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus) {
        List<Merchant> merchants = findMerchantsByFilter(status, kycStatus);
        return ResponseEntity.ok(ApiResponse.success(merchants));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Merchant>> updateMerchant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantRequest request) {
        Merchant updates = mapToUpdateMerchant(request);
        Merchant updated = merchantService.updateMerchant(id, updates);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/kyc/verify")
    public ResponseEntity<ApiResponse<Merchant>> verifyKyc(
            @PathVariable UUID id,
            @Valid @RequestBody KycVerificationRequest request) {
        Merchant updated = merchantService.updateKycStatus(id, "VERIFIED", "SYSTEM", request.notes());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/kyc/reject")
    public ResponseEntity<ApiResponse<Merchant>> rejectKyc(
            @PathVariable UUID id,
            @Valid @RequestBody KycRejectionRequest request) {
        Merchant updated = merchantService.updateKycStatus(id, "REJECTED", "SYSTEM", request.reason());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Merchant>> suspendMerchant(@PathVariable UUID id) {
        Merchant suspended = merchantService.suspendMerchant(id);
        return ResponseEntity.ok(ApiResponse.success(suspended));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Merchant>> activateMerchant(@PathVariable UUID id) {
        Merchant activated = merchantService.activateMerchant(id);
        return ResponseEntity.ok(ApiResponse.success(activated));
    }

    @PostMapping("/{id}/regenerate-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> regenerateApiKey(@PathVariable UUID id) {
        merchantService.regenerateApiKey(id);
        return merchantService.getMerchant(id)
                .map(m -> ResponseEntity.ok(ApiResponse.success(Map.of(
                        "apiKey", m.getApiKey(),
                        "message", "API key regenerated successfully"
                ))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<ApiResponse<List<KycDocument>>> getKycDocuments(@PathVariable UUID id) {
        List<KycDocument> documents = merchantService.getKycDocuments(id);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/{id}/kyc-documents")
    public ResponseEntity<ApiResponse<KycDocument>> addKycDocument(
            @PathVariable UUID id,
            @Valid @RequestBody AddKycDocumentRequest request) {
        KycDocument document = mapToKycDocument(request);
        KycDocument created = merchantService.addKycDocument(id, document);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PostMapping("/{id}/kyc/submit")
    public ResponseEntity<ApiResponse<Merchant>> submitKyc(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitKycRequest request) {
        List<KycDocument> documents = request.documents().stream()
                .map(this::mapToKycDocument)
                .toList();
        Merchant updated = kycService.submitKyc(id, documents);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/kyc/start-review")
    public ResponseEntity<ApiResponse<Merchant>> startKycReview(
            @PathVariable UUID id,
            @RequestBody KycReviewRequest request) {
        Merchant updated = kycService.startReview(id, request.reviewer());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @GetMapping("/{id}/kyc/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKycStatus(@PathVariable UUID id) {
        KycStatus status = kycService.getKycStatus(id);
        boolean canAcceptPayments = kycService.canAcceptPayments(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", status.name(),
                "description", status.getDescription(),
                "canAcceptPayments", canAcceptPayments
        )));
    }

    @GetMapping("/{id}/bank-accounts")
    public ResponseEntity<ApiResponse<List<BankAccount>>> getBankAccounts(@PathVariable UUID id) {
        List<BankAccount> accounts = merchantService.getBankAccounts(id);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping("/{id}/bank-accounts")
    public ResponseEntity<ApiResponse<BankAccount>> addBankAccount(
            @PathVariable UUID id,
            @Valid @RequestBody AddBankAccountRequest request) {
        BankAccount account = mapToBankAccount(request);
        BankAccount created = merchantService.addBankAccount(id, account);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}/bank-accounts/default")
    public ResponseEntity<ApiResponse<BankAccount>> getDefaultBankAccount(@PathVariable UUID id) {
        BankAccount account = merchantService.getDefaultBankAccount(id);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/{merchantId}/bank-accounts/{accountId}/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultBankAccount(
            @PathVariable UUID merchantId,
            @PathVariable UUID accountId) {
        merchantService.setDefaultBankAccount(merchantId, accountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKey>>> getApiKeys(@PathVariable UUID merchantId) {
        List<ApiKey> keys = apiKeyService.getMerchantApiKeys(merchantId);
        return ResponseEntity.ok(ApiResponse.success(keys));
    }

    @PostMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createApiKey(
            @PathVariable UUID merchantId,
            @Valid @RequestBody CreateApiKeyRequest request) {
        Instant expiresAt = request.expiresAt() != null ? Instant.parse(request.expiresAt()) : null;
        var result = apiKeyService.createApiKey(
                merchantId,
                request.name(),
                request.description(),
                request.permissions(),
                request.rateLimitPerMinute(),
                null,
                expiresAt
        );
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "apiKey", result.rawKey(),
                "id", result.apiKey().getId(),
                "name", result.apiKey().getName(),
                "prefix", result.apiKey().getKeyPrefix(),
                "createdAt", result.apiKey().getCreatedAt().toString()
        )));
    }

    @PostMapping("/{merchantId}/api-keys/{apiKeyId}/rotate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotateApiKey(
            @PathVariable UUID merchantId,
            @PathVariable UUID apiKeyId) {
        var result = apiKeyService.rotateApiKey(apiKeyId);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "apiKey", result.rawKey(),
                "id", result.apiKey().getId(),
                "message", "API key rotated successfully"
        )));
    }

    @PostMapping("/{merchantId}/api-keys/{apiKeyId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(
            @PathVariable UUID merchantId,
            @PathVariable UUID apiKeyId) {
        apiKeyService.revokeApiKey(apiKeyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private List<Merchant> findMerchantsByFilter(String status, String kycStatus) {
        if (status != null) {
            return merchantService.getMerchantsByStatus(status);
        }
        if (kycStatus != null) {
            return merchantService.getMerchantsByKycStatus(kycStatus);
        }
        return merchantService.getAllMerchants();
    }

    private Merchant mapToMerchant(CreateMerchantRequest request) {
        Merchant merchant = new Merchant();
        merchant.setBusinessName(request.businessName());
        merchant.setLegalName(request.legalName());
        merchant.setEmail(request.email());
        merchant.setPhone(request.phone());
        merchant.setWebsite(request.website());
        merchant.setBusinessType(request.businessType());
        merchant.setBusinessCategory(request.businessCategory());
        merchant.setTaxId(request.taxId());
        return merchant;
    }

    private Merchant mapToUpdateMerchant(UpdateMerchantRequest request) {
        Merchant merchant = new Merchant();
        merchant.setBusinessName(request.businessName());
        merchant.setLegalName(request.legalName());
        merchant.setPhone(request.phone());
        merchant.setWebsite(request.website());
        merchant.setBusinessType(request.businessType());
        merchant.setWebhookUrl(request.webhookUrl());
        return merchant;
    }

    private KycDocument mapToKycDocument(AddKycDocumentRequest request) {
        KycDocument document = new KycDocument();
        document.setDocumentType(request.documentType());
        document.setDocumentNumber(request.documentNumber());
        document.setFileUrl(request.fileUrl());
        document.setFileKey(request.fileKey());
        return document;
    }

    private KycDocument mapToKycDocument(SubmitKycRequest.KycDocumentDto dto) {
        KycDocument document = new KycDocument();
        document.setDocumentType(dto.documentType());
        document.setDocumentNumber(dto.documentNumber());
        document.setFileUrl(dto.fileUrl());
        document.setFileKey(dto.fileKey());
        return document;
    }

    private BankAccount mapToBankAccount(AddBankAccountRequest request) {
        BankAccount account = new BankAccount();
        account.setAccountHolderName(request.accountHolderName());
        account.setBankName(request.bankName());
        account.setAccountNumber(request.accountNumber());
        account.setAccountType(request.accountType());
        account.setIfscCode(request.ifscCode());
        account.setRoutingNumber(request.routingNumber());
        account.setSwiftCode(request.swiftCode());
        account.setIsDefault(request.isDefault());
        return account;
    }
}
