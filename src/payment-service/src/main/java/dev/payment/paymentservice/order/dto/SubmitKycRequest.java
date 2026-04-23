package dev.payment.paymentservice.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitKycRequest(
        @NotEmpty(message = "At least one document is required")
        List<KycDocumentDto> documents
) {
    public record KycDocumentDto(
            String documentType,
            String documentNumber,
            String fileUrl,
            String fileKey
    ) {
    }
}
