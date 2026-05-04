package dev.payment.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyOtpRequest {
    @NotBlank
    private String otp;

    private String transactionId;
}
