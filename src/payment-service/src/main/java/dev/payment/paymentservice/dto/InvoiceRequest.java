package dev.payment.paymentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceRequest {
    @NotBlank @Email String customerEmail;
    String customerName;
    @NotNull @Positive BigDecimal amount;
    @NotBlank String currency;
    String description;
    Instant dueDate;
    String lineItems;
}
