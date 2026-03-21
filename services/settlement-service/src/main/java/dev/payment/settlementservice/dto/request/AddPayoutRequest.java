package dev.payment.settlementservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddPayoutRequest(@NotBlank String beneficiaryAccount, @NotNull @DecimalMin("0.01") BigDecimal amount) {}
