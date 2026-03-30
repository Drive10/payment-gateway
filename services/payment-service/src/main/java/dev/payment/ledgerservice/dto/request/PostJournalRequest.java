package dev.payment.ledgerservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PostJournalRequest(
        @NotBlank String reference,
        @NotBlank String debitAccountCode,
        @NotBlank String creditAccountCode,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String narration
) {
}
