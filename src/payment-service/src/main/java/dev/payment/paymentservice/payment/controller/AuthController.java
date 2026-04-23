package dev.payment.paymentservice.payment.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.paymentservice.payment.dto.request.SyncUserRequest;
import dev.payment.paymentservice.payment.dto.response.UserResponse;
import dev.payment.paymentservice.payment.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/platform/auth")
@Tag(name = "Internal Authentication", description = "Internal authentication endpoints for service-to-service communication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sync")
    public ApiResponse<UserResponse> syncUser(@Valid @RequestBody SyncUserRequest request) {
        return ApiResponse.success(authService.syncUser(request.email()));
    }
}
