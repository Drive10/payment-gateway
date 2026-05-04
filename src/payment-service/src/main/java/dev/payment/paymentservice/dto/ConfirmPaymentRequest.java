package dev.payment.paymentservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentRequest {
    private String paymentMethod;
    private String cardToken;
    private String upiId;
    private String bankCode;
    private String idempotencyKey;
}
