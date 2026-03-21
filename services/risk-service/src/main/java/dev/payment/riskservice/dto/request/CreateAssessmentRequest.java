package dev.payment.riskservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAssessmentRequest(@NotBlank String merchantReference, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotBlank String countryCode, int velocityCount) {}
