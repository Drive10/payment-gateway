package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.ApiResponse;
import dev.payment.paymentservice.dto.SavePaymentMethodRequest;
import dev.payment.paymentservice.entity.SavedPaymentMethod;
import dev.payment.paymentservice.service.VaultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/vault", "/api/v1/vault"})
@RequiredArgsConstructor
@Tag(name = "Vault", description = "Saved payment methods (customer vault)")
public class VaultController {
    private final VaultService vaultService;

    @PostMapping("/methods")
    @Operation(summary = "Save a payment method")
    public ResponseEntity<ApiResponse<SavedPaymentMethod>> saveMethod(@Valid @RequestBody SavePaymentMethodRequest request) {
        SavedPaymentMethod method = vaultService.savePaymentMethod(request);
        return ResponseEntity.ok(ApiResponse.success(method));
    }

    @GetMapping("/methods")
    @Operation(summary = "List customer payment methods")
    public ResponseEntity<ApiResponse<List<SavedPaymentMethod>>> listMethods(@RequestParam String customerEmail) {
        List<SavedPaymentMethod> methods = vaultService.getCustomerPaymentMethods(customerEmail);
        return ResponseEntity.ok(ApiResponse.success(methods));
    }

    @GetMapping("/methods/{token}")
    @Operation(summary = "Get payment method by token")
    public ResponseEntity<ApiResponse<SavedPaymentMethod>> getMethod(@PathVariable String token) {
        SavedPaymentMethod method = vaultService.getPaymentMethod(token);
        return ResponseEntity.ok(ApiResponse.success(method));
    }

    @DeleteMapping("/methods/{token}")
    @Operation(summary = "Delete a saved payment method")
    public ResponseEntity<ApiResponse<Void>> deleteMethod(@PathVariable String token) {
        vaultService.deletePaymentMethod(token);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/methods/{token}/default")
    @Operation(summary = "Set payment method as default")
    public ResponseEntity<ApiResponse<SavedPaymentMethod>> setDefault(@PathVariable String token) {
        SavedPaymentMethod method = vaultService.setDefault(token);
        return ResponseEntity.ok(ApiResponse.success(method));
    }
}
