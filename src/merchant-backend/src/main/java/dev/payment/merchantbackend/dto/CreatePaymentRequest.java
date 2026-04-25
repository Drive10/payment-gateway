package dev.payment.merchantbackend.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {
    @NotBlank
    private String orderId;
    
    @NotNull
    @Positive
    private Integer amount;
    
    @NotBlank
    private String currency = "INR";
    
    private String paymentMethod = "CARD";
    private String upiId;
}
