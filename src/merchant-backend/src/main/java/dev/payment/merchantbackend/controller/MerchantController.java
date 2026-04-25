package dev.payment.merchantbackend.controller;

import dev.payment.merchantbackend.dto.*;
import dev.payment.merchantbackend.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantController {
    private final MerchantService merchantService;

    @PostMapping("/create-payment")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Authorization") String jwt,
            @RequestHeader("X-Merchant-Id") String merchantId) {
        
        CreatePaymentResponse response = merchantService.createPayment(request, jwt, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
