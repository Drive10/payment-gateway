package dev.payment.combinedservice.auth.controller;

import dev.payment.combinedservice.auth.dto.UserResponse;
import dev.payment.combinedservice.auth.entity.User;
import dev.payment.combinedservice.auth.service.AuthService;
import dev.payment.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/platform/auth")
public class PlatformUserController {

    private final AuthService authService;

    public PlatformUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users/{email}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByEmail(@PathVariable String email) {
        User user = authService.getUserByEmail(email);
        UserResponse response = UserResponse.from(user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
