package dev.payment.ledgerservice.dto.request;

import dev.payment.ledgerservice.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotBlank String accountCode,
        @NotBlank String accountName,
        @NotNull AccountType type
) {
}
