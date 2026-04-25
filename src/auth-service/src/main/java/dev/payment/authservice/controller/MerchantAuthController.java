package dev.payment.authservice.controller;

import dev.payment.authservice.dto.*;
import dev.payment.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/merchant/auth")
@RequiredArgsConstructor
public class MerchantAuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody MerchantRegisterRequest request) {
        TokenResponse response = authService.registerMerchant(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.merchantLogin(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/validate-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateKey(@RequestHeader("X-API-Key") String apiKey) {
        var merchant = authService.validateApiKey(apiKey);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "merchantId", merchant.getId().toString(),
            "status", merchant.getStatus().name()
        )));
    }

    @GetMapping("/get-api-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> getApiKey(@RequestHeader("Authorization") String jwt, @RequestParam("merchantId") String merchantId) {
        var apiKey = authService.getApiKeyForMerchant(merchantId, jwt);
        return ResponseEntity.ok(ApiResponse.success(Map.of("apiKey", apiKey)));
    }

    @GetMapping("/details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetails(@RequestHeader("Authorization") String jwt, @RequestParam("merchantId") String merchantId) {
        var merchant = authService.getMerchantDetails(merchantId, jwt);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "id", merchant.getId().toString(),
            "businessName", merchant.getBusinessName(),
            "webhookUrl", merchant.getWebhookUrl(),
            "status", merchant.getStatus().name()
        )));
    }
}
