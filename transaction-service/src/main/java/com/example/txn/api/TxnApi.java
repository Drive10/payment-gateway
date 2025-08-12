package com.example.txn.api;

import com.example.txn.model.Transaction;
import com.example.txn.service.TxnService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class TxnApi {
  private final TxnService service;

  public TxnApi(TxnService service) { this.service = service; }

  // called by orchestration to create a PENDING txn
  @PostMapping("/internal/payments/{id}")
  public ResponseEntity<Void> create(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
    UUID userId = UUID.fromString((String) body.get("userId"));
    BigDecimal amount = new BigDecimal(body.get("amount").toString());
    String currency = (String) body.get("currency");
    service.create(id, userId, amount, currency);
    return ResponseEntity.accepted().build();
  }

  // public read
  @GetMapping("/payments/{id}")
  public ResponseEntity<Transaction> get(@PathVariable UUID id){
    Optional<Transaction> txn = service.get(id);
    return txn.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  // temp simulate success
  @PostMapping("/internal/payments/{id}/succeed")
  public ResponseEntity<Void> succeed(@PathVariable UUID id){
    service.succeed(id);
    return ResponseEntity.accepted().build();
  }
}
