package dev.payment.riskservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.riskservice.domain.Decision;
import dev.payment.riskservice.dto.request.CreateAssessmentRequest;
import dev.payment.riskservice.dto.response.AssessmentResponse;
import dev.payment.riskservice.service.RiskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {
    private final RiskService riskService;
    public RiskController(RiskService riskService) { this.riskService = riskService; }

    @PostMapping("/assessments")
    public ApiResponse<AssessmentResponse> assess(@Valid @RequestBody CreateAssessmentRequest request) {
        return ApiResponse.success(riskService.assess(request));
    }

    @GetMapping("/assessments")
    public ApiResponse<List<AssessmentResponse>> getAssessments(@RequestParam(required = false) Decision decision) {
        return ApiResponse.success(riskService.getAssessments(decision));
    }
}
