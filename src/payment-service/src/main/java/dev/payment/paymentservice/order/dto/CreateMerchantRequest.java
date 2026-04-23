package dev.payment.paymentservice.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMerchantRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Business name is required")
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @Size(max = 255, message = "Legal name must not exceed 255 characters")
        String legalName,

        @Size(max = 50, message = "Phone must not exceed 50 characters")
        String phone,

        @Size(max = 255, message = "Website must not exceed 255 characters")
        String website,

        @Size(max = 100, message = "Business type must not exceed 100 characters")
        String businessType,

        @Size(max = 100, message = "Business category must not exceed 100 characters")
        String businessCategory,

        @Size(max = 100, message = "Tax ID must not exceed 100 characters")
        String taxId
) {
}
