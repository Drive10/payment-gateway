package com.example.orch.api;

import com.example.common.dto.PaymentDtos.CreatePaymentRequest;
import com.example.common.dto.PaymentDtos.CreatePaymentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {
  private final RestClient client = RestClient.create("http://localhost:8082");

  @PostMapping
  public ResponseEntity<CreatePaymentResponse> create(@RequestBody CreatePaymentRequest req) {
    // generate a payment id here (idempotency to be added later)
    UUID paymentId = UUID.randomUUID();
    // tell transaction-service to create a PENDING record (HTTP call)
    client.post()
          .uri("/internal/payments/" + paymentId)
          .body(req)
          .retrieve()
          .toBodilessEntity();
    return ResponseEntity.accepted().body(new CreatePaymentResponse(paymentId, "PENDING"));
  }
}
