package dev.payment.paymentservice.order.dto;

import jakarta.validation.constraints.NotBlank;

public record AddKycDocumentRequest(
        @NotBlank(message = "Document type is required")
        String documentType,

        String documentNumber,
        String fileUrl,
        String fileKey
) {
}
