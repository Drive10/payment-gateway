package dev.payment.paymentservice.controller;

import dev.payment.paymentservice.dto.ApiResponse;
import dev.payment.paymentservice.dto.PlanRequest;
import dev.payment.paymentservice.entity.Plan;
import dev.payment.paymentservice.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/plans", "/api/v1/plans"})
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Subscription plan management")
public class PlanController {
    private final PlanService planService;

    private String getMerchantId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal instanceof String s ? s : "merchant_default";
    }

    @PostMapping
    @Operation(summary = "Create a subscription plan")
    public ResponseEntity<ApiResponse<Plan>> createPlan(@Valid @RequestBody PlanRequest request) {
        Plan plan = planService.createPlan(request, getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping
    @Operation(summary = "List merchant plans")
    public ResponseEntity<ApiResponse<List<Plan>>> listPlans() {
        List<Plan> plans = planService.getMerchantPlans(getMerchantId());
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Get plan details")
    public ResponseEntity<ApiResponse<Plan>> getPlan(@PathVariable String planId) {
        Plan plan = planService.getPlan(planId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PutMapping("/{planId}")
    @Operation(summary = "Update a plan")
    public ResponseEntity<ApiResponse<Plan>> updatePlan(@PathVariable String planId, @Valid @RequestBody PlanRequest request) {
        Plan plan = planService.updatePlan(planId, request);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @PostMapping("/{planId}/deactivate")
    @Operation(summary = "Deactivate a plan")
    public ResponseEntity<ApiResponse<Plan>> deactivatePlan(@PathVariable String planId) {
        Plan plan = planService.deactivatePlan(planId);
        return ResponseEntity.ok(ApiResponse.success(plan));
    }
}
