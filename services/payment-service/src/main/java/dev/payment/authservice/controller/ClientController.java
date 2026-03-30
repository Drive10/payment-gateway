package dev.payment.authservice.controller;

import dev.payment.authservice.domain.ClientStatus;
import dev.payment.authservice.dto.request.CreateApiClientRequest;
import dev.payment.authservice.dto.request.VerifyClientRequest;
import dev.payment.authservice.dto.response.ApiClientResponse;
import dev.payment.authservice.dto.response.VerificationResponse;
import dev.payment.authservice.service.ClientCredentialsService;
import dev.payment.common.api.ApiResponse;
import dev.payment.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientCredentialsService clientCredentialsService;

    public ClientController(ClientCredentialsService clientCredentialsService) {
        this.clientCredentialsService = clientCredentialsService;
    }

    @PostMapping
    public ApiResponse<ApiClientResponse> createClient(@Valid @RequestBody CreateApiClientRequest request) {
        return ApiResponse.success(clientCredentialsService.createClient(request));
    }

    @PatchMapping("/{clientCode}/rotate-key")
    public ApiResponse<ApiClientResponse> rotateKey(@PathVariable String clientCode) {
        return ApiResponse.success(clientCredentialsService.rotateKey(clientCode));
    }

    @PostMapping("/verify")
    public ApiResponse<VerificationResponse> verify(@Valid @RequestBody VerifyClientRequest request) {
        return ApiResponse.success(clientCredentialsService.verify(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<ApiClientResponse>> getClients(
            @RequestParam(required = false) ClientStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(PageResponse.from(clientCredentialsService.getClients(status, PageRequest.of(page, Math.min(size, 100)))));
    }
}
