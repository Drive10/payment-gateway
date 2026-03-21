package dev.payment.authservice.service;

import dev.payment.authservice.domain.AccessAudit;
import dev.payment.authservice.domain.ApiClient;
import dev.payment.authservice.domain.ClientStatus;
import dev.payment.authservice.dto.request.CreateApiClientRequest;
import dev.payment.authservice.dto.request.VerifyClientRequest;
import dev.payment.authservice.dto.response.ApiClientResponse;
import dev.payment.authservice.dto.response.VerificationResponse;
import dev.payment.authservice.exception.ApiException;
import dev.payment.authservice.repository.AccessAuditRepository;
import dev.payment.authservice.repository.ApiClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ClientCredentialsService {

    private final ApiClientRepository apiClientRepository;
    private final AccessAuditRepository accessAuditRepository;

    public ClientCredentialsService(ApiClientRepository apiClientRepository, AccessAuditRepository accessAuditRepository) {
        this.apiClientRepository = apiClientRepository;
        this.accessAuditRepository = accessAuditRepository;
    }

    @Transactional
    public ApiClientResponse createClient(CreateApiClientRequest request) {
        if (apiClientRepository.existsByClientCode(request.clientCode())) {
            throw new ApiException(HttpStatus.CONFLICT, "CLIENT_EXISTS", "Client code already exists");
        }
        ApiClient client = new ApiClient();
        client.setClientCode(request.clientCode().toUpperCase());
        client.setDisplayName(request.displayName());
        client.setWebhookUrl(request.webhookUrl());
        client.setScopes(request.scopes());
        client.setStatus(ClientStatus.ACTIVE);
        client.setApiKey(generateApiKey(request.clientCode()));
        apiClientRepository.save(client);
        audit(client.getClientCode(), "CLIENT_CREATED", "Credential issued");
        return toResponse(client);
    }

    @Transactional
    public ApiClientResponse rotateKey(String clientCode) {
        ApiClient client = apiClientRepository.findByClientCode(clientCode.toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "Client not found"));
        client.setApiKey(generateApiKey(client.getClientCode()));
        client.setStatus(ClientStatus.ROTATED);
        apiClientRepository.save(client);
        audit(client.getClientCode(), "KEY_ROTATED", "API key rotated");
        return toResponse(client);
    }

    public VerificationResponse verify(VerifyClientRequest request) {
        ApiClient client = apiClientRepository.findByApiKey(request.apiKey())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_KEY", "API key is invalid"));
        audit(client.getClientCode(), "KEY_VERIFIED", "Verification passed");
        return new VerificationResponse(true, client.getClientCode(), client.getStatus().name(), client.getScopes());
    }

    public Page<ApiClientResponse> getClients(ClientStatus status, Pageable pageable) {
        return (status == null ? apiClientRepository.findAll(pageable) : apiClientRepository.findByStatus(status, pageable))
                .map(this::toResponse);
    }

    private void audit(String clientCode, String action, String outcome) {
        AccessAudit audit = new AccessAudit();
        audit.setClientCode(clientCode);
        audit.setAction(action);
        audit.setOutcome(outcome);
        accessAuditRepository.save(audit);
    }

    private ApiClientResponse toResponse(ApiClient client) {
        return new ApiClientResponse(
                client.getId(),
                client.getClientCode(),
                client.getDisplayName(),
                client.getApiKey(),
                client.getWebhookUrl(),
                client.getScopes(),
                client.getStatus().name(),
                client.getCreatedAt()
        );
    }

    private String generateApiKey(String clientCode) {
        return "pk_" + clientCode.toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
