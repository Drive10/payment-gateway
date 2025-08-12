package com.example.common.dto;

import java.math.BigDecimal;
import java.util.UUID;

public final class PaymentDtos {

  public record CreatePaymentRequest(
          UUID userId,
          BigDecimal amount,
          String currency
  ) {}

  public record CreatePaymentResponse(
          UUID paymentId,
          String status
  ) {}
}
