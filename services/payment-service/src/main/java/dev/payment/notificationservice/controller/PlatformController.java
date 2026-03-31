package dev.payment.notificationservice.controller;

import dev.payment.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/platform/notification")
public class PlatformController {

    @GetMapping("/status")
    public ApiResponse<String> status() {
        return ApiResponse.success("notification-service delivery domain is operational");
    }
}
