package dev.payment.paymentservice.dto;

import lombok.*;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLinkResponse {
    private String referenceId;
    private String checkoutUrl;
    private String amount;
    private String currency;
    private String merchantName;
    private String description;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
}