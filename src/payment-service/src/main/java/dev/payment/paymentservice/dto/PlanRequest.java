package dev.payment.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanRequest {
    @NotBlank String name;
    @NotBlank String description;
    @NotNull @Positive BigDecimal amount;
    @NotBlank String currency;
    @NotBlank String interval;
    Integer trialPeriodDays;
}
