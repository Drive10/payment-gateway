package dev.payment.paymentservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponse {
    private String refundId;
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private BigDecimal refundedAmount;
    private String currency;
    private String status;
    private String reason;
    private Instant createdAt;
}