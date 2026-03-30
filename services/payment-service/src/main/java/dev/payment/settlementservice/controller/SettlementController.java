package dev.payment.settlementservice.controller;

import dev.payment.common.api.ApiResponse;
import dev.payment.settlementservice.dto.request.AddPayoutRequest;
import dev.payment.settlementservice.dto.request.CreateBatchRequest;
import dev.payment.settlementservice.dto.response.SettlementBatchResponse;
import dev.payment.settlementservice.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settlements")
public class SettlementController {
    private final SettlementService settlementService;
    public SettlementController(SettlementService settlementService) { this.settlementService = settlementService; }

    @PostMapping
    public ApiResponse<SettlementBatchResponse> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        return ApiResponse.success(settlementService.createBatch(request));
    }

    @PostMapping("/{batchReference}/payouts")
    public ApiResponse<SettlementBatchResponse> addPayout(@PathVariable String batchReference, @Valid @RequestBody AddPayoutRequest request) {
        return ApiResponse.success(settlementService.addPayout(batchReference, request));
    }

    @PostMapping("/{batchReference}/execute")
    public ApiResponse<SettlementBatchResponse> execute(@PathVariable String batchReference) {
        return ApiResponse.success(settlementService.execute(batchReference));
    }

    @GetMapping
    public ApiResponse<List<SettlementBatchResponse>> getBatches() {
        return ApiResponse.success(settlementService.getBatches());
    }
}
