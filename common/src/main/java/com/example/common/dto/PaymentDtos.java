package com.example.common.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

public class PaymentDtos {
  @Data @NoArgsConstructor @AllArgsConstructor @Builder
  public static class CreatePaymentRequest {
    @NotNull private UUID userId;
    @NotNull @DecimalMin("0.1") private BigDecimal amount;
    @NotBlank private String currency;
  }
  @Data @NoArgsConstructor @AllArgsConstructor @Builder
  public static class CreatePaymentResponse {
    private UUID paymentId;
    private String status;
  }
}
