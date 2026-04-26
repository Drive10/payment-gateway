package dev.payment.merchantbackend.controller;

import dev.payment.merchantbackend.dto.*;
import dev.payment.merchantbackend.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant", description = "API for merchant operations")
public class MerchantController {
    private final MerchantService merchantService;

    @PostMapping("/create-payment")
    @Operation(summary = "Create payment", description = "Create a payment from merchant backend")
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader("Authorization") String jwt,
            @RequestHeader("X-Merchant-Id") String merchantId) {

        CreatePaymentResponse response = merchantService.createPayment(request, jwt, merchantId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
