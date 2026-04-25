package dev.payment.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @Positive(message = "Refund amount must be positive")
    private BigDecimal amount;

    private String reason;
}