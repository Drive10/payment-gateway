package dev.payment.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AddBankAccountRequest(
        @NotBlank(message = "Account holder name is required")
        String accountHolderName,

        @NotBlank(message = "Bank name is required")
        String bankName,

        @NotBlank(message = "Account number is required")
        String accountNumber,

        @NotBlank(message = "Account type is required")
        String accountType,

        String ifscCode,
        String routingNumber,
        String swiftCode,
        Boolean isDefault
) {
    public AddBankAccountRequest {
        if (isDefault == null) {
            isDefault = false;
        }
    }
}
