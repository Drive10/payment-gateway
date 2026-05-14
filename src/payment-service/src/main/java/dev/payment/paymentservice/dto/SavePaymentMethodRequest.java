package dev.payment.paymentservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavePaymentMethodRequest {
    @NotBlank @Email String customerEmail;
    @NotBlank String type;
    String cardNumber;
    String cardholderName;
    String expiryMonth;
    String expiryYear;
    String cardBrand;
    String upiId;
    boolean isDefault;
}
