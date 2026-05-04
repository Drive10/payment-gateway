package dev.payment.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentResponse {
    private String paymentId;
    private String transactionId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String checkoutUrl;
    private String merchantId;
    private String clientSecret;
    private Boolean requiresAction;
    private String nextAction;
    private String message;
}
