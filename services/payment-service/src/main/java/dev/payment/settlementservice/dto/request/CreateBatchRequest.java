package dev.payment.settlementservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateBatchRequest(@NotBlank String batchReference) {}
