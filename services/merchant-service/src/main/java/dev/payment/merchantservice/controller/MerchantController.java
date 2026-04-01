package dev.payment.merchantservice.controller;

import dev.payment.merchantservice.entity.ApiKey;
import dev.payment.merchantservice.entity.BankAccount;
import dev.payment.merchantservice.entity.KycDocument;
import dev.payment.merchantservice.entity.KycStatus;
import dev.payment.merchantservice.entity.Merchant;
import dev.payment.merchantservice.service.MerchantService;
import dev.payment.merchantservice.service.KycService;
import dev.payment.merchantservice.service.ApiKeyService;
import dev.payment.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
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
    public ResponseEntity<ApiResponse<Merchant>> createMerchant(@RequestBody Map<String, Object> request) {
        validateCreateRequest(request);

        Merchant merchant = new Merchant();
        merchant.setBusinessName(getString(request, "businessName"));
        merchant.setLegalName(getString(request, "legalName"));
        merchant.setEmail(getStringRequired(request, "email"));
        merchant.setPhone(getString(request, "phone"));
        merchant.setWebsite(getString(request, "website"));
        merchant.setBusinessType(getString(request, "businessType"));
        merchant.setBusinessCategory(getString(request, "businessCategory"));
        merchant.setTaxId(getString(request, "taxId"));

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

        List<Merchant> merchants;
        if (status != null) {
            merchants = merchantService.getMerchantsByStatus(status);
        } else if (kycStatus != null) {
            merchants = merchantService.getMerchantsByKycStatus(kycStatus);
        } else {
            merchants = merchantService.getAllMerchants();
        }

        return ResponseEntity.ok(ApiResponse.success(merchants));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Merchant>> updateMerchant(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        Merchant updates = new Merchant();
        updates.setBusinessName(getString(request, "businessName"));
        updates.setLegalName(getString(request, "legalName"));
        updates.setPhone(getString(request, "phone"));
        updates.setWebsite(getString(request, "website"));
        updates.setBusinessType(getString(request, "businessType"));
        updates.setWebhookUrl(getString(request, "webhookUrl"));

        Merchant updated = merchantService.updateMerchant(id, updates);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/kyc/verify")
    public ResponseEntity<ApiResponse<Merchant>> verifyKyc(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        String verifiedBy = getStringOrDefault(request, "verifiedBy", "SYSTEM");
        String notes = getString(request, "notes");

        Merchant updated = merchantService.updateKycStatus(id, "VERIFIED", verifiedBy, notes);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/kyc/reject")
    public ResponseEntity<ApiResponse<Merchant>> rejectKyc(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        String reason = getStringRequired(request, "reason");

        Merchant updated = merchantService.updateKycStatus(id, "REJECTED", "SYSTEM", reason);
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

    // KYC Documents
    @GetMapping("/{id}/kyc-documents")
    public ResponseEntity<ApiResponse<List<KycDocument>>> getKycDocuments(@PathVariable UUID id) {
        List<KycDocument> documents = merchantService.getKycDocuments(id);
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @PostMapping("/{id}/kyc-documents")
    public ResponseEntity<ApiResponse<KycDocument>> addKycDocument(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        KycDocument document = new KycDocument();
        document.setDocumentType(getStringRequired(request, "documentType"));
        document.setDocumentNumber(getString(request, "documentNumber"));
        document.setFileUrl(getString(request, "fileUrl"));
        document.setFileKey(getString(request, "fileKey"));

        KycDocument created = merchantService.addKycDocument(id, document);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PostMapping("/{id}/kyc/submit")
    public ResponseEntity<ApiResponse<Merchant>> submitKyc(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        List<KycDocument> documents = parseDocuments(request);
        Merchant updated = kycService.submitKyc(id, documents);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PostMapping("/{id}/kyc/start-review")
    public ResponseEntity<ApiResponse<Merchant>> startKycReview(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        String reviewer = getStringOrDefault(request, "reviewer", "SYSTEM");
        Merchant updated = kycService.startReview(id, reviewer);
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

    // Bank Accounts
    @GetMapping("/{id}/bank-accounts")
    public ResponseEntity<ApiResponse<List<BankAccount>>> getBankAccounts(@PathVariable UUID id) {
        List<BankAccount> accounts = merchantService.getBankAccounts(id);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @PostMapping("/{id}/bank-accounts")
    public ResponseEntity<ApiResponse<BankAccount>> addBankAccount(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {

        validateBankAccountRequest(request);

        BankAccount account = new BankAccount();
        account.setAccountHolderName(getStringRequired(request, "accountHolderName"));
        account.setBankName(getStringRequired(request, "bankName"));
        account.setAccountNumber(getStringRequired(request, "accountNumber"));
        account.setAccountType(getStringRequired(request, "accountType"));
        account.setIfscCode(getString(request, "ifscCode"));
        account.setRoutingNumber(getString(request, "routingNumber"));
        account.setSwiftCode(getString(request, "swiftCode"));
        account.setIsDefault(getBoolean(request, "isDefault", false));

        BankAccount created = merchantService.addBankAccount(id, account);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @GetMapping("/{id}/bank-accounts/default")
    public ResponseEntity<ApiResponse<BankAccount>> getDefaultBankAccount(@PathVariable UUID id) {
        BankAccount account = merchantService.getDefaultBankAccount(id);
        if (account == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/{merchantId}/bank-accounts/{accountId}/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultBankAccount(
            @PathVariable UUID merchantId,
            @PathVariable UUID accountId) {

        merchantService.setDefaultBankAccount(merchantId, accountId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // API Keys
    @GetMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKey>>> getApiKeys(@PathVariable UUID merchantId) {
        List<ApiKey> keys = apiKeyService.getMerchantApiKeys(merchantId);
        return ResponseEntity.ok(ApiResponse.success(keys));
    }

    @PostMapping("/{merchantId}/api-keys")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createApiKey(
            @PathVariable UUID merchantId,
            @RequestBody Map<String, Object> request) {
        String name = getStringRequired(request, "name");
        String description = getString(request, "description");
        List<String> permissions = (List<String>) request.get("permissions");
        Integer rateLimit = getInt(request, "rateLimitPerMinute");
        Instant expiresAt = request.get("expiresAt") != null 
                ? Instant.parse(request.get("expiresAt").toString()) 
                : null;

        var result = apiKeyService.createApiKey(merchantId, name, description, 
                permissions, rateLimit, null, expiresAt);

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

    // Helper methods
    private void validateCreateRequest(Map<String, Object> request) {
        if (!request.containsKey("email") || request.get("email") == null) {
            throw new IllegalArgumentException("email is required");
        }
        if (!request.containsKey("businessName") || request.get("businessName") == null) {
            throw new IllegalArgumentException("businessName is required");
        }
    }

    private void validateBankAccountRequest(Map<String, Object> request) {
        if (!request.containsKey("accountHolderName") || request.get("accountHolderName") == null) {
            throw new IllegalArgumentException("accountHolderName is required");
        }
        if (!request.containsKey("bankName") || request.get("bankName") == null) {
            throw new IllegalArgumentException("bankName is required");
        }
        if (!request.containsKey("accountNumber") || request.get("accountNumber") == null) {
            throw new IllegalArgumentException("accountNumber is required");
        }
        if (!request.containsKey("accountType") || request.get("accountType") == null) {
            throw new IllegalArgumentException("accountType is required");
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getStringRequired(Map<String, Object> map, String key) {
        String value = getString(map, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return null;
    }

    private Boolean getBoolean(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        return value != null ? (Boolean) value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<KycDocument> parseDocuments(Map<String, Object> request) {
        List<KycDocument> documents = new ArrayList<>();
        Object docsObj = request.get("documents");
        if (docsObj instanceof List<?>) {
            for (Object docObj : (List<?>) docsObj) {
                if (docObj instanceof Map) {
                    KycDocument doc = new KycDocument();
                    doc.setDocumentType(getString((Map<String, Object>) docObj, "documentType"));
                    doc.setDocumentNumber(getString((Map<String, Object>) docObj, "documentNumber"));
                    doc.setFileUrl(getString((Map<String, Object>) docObj, "fileUrl"));
                    doc.setFileKey(getString((Map<String, Object>) docObj, "fileKey"));
                    documents.add(doc);
                }
            }
        }
        return documents;
    }
}
