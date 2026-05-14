package dev.payment.paymentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionRequest {
    @NotBlank String planId;
    @NotBlank @Email String customerEmail;
    String customerName;
    String paymentMethodToken;
    Integer trialPeriodDays;
    Integer billingCycles;
}
