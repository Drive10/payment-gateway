package dev.payment.notificationservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.notificationservice.dto.request.CreateTemplateRequest;
import dev.payment.notificationservice.dto.request.SendNotificationRequest;
import dev.payment.notificationservice.dto.response.NotificationResponse;
import dev.payment.notificationservice.dto.response.TemplateResponse;
import dev.payment.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }

    @PostMapping("/templates")
    public ApiResponse<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        return ApiResponse.success(notificationService.createTemplate(request));
    }

    @PostMapping
    public ApiResponse<NotificationResponse> send(@Valid @RequestBody SendNotificationRequest request) {
        return ApiResponse.success(notificationService.send(request));
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getDelivered() {
        return ApiResponse.success(notificationService.getDelivered());
    }
}
