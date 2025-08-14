package dev.payment.paymentservice.api;

import dev.payment.common.dto.PaymentDTO;
import dev.payment.paymentservice.application.PaymentApplicationService;
import dev.payment.paymentservice.domain.Payment;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile("legacy")
@RestController
@RequestMapping("/payments")
public class PaymentController {
  private final PaymentApplicationService app;

  public PaymentController(PaymentApplicationService app) {
    this.app = app;
  }

  @PostMapping
  public ResponseEntity<Payment> create(@Valid @RequestBody PaymentDTO dto) {
    return ResponseEntity.ok(app.create(dto));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Payment> get(@PathVariable String id) {
    return app.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }
}
