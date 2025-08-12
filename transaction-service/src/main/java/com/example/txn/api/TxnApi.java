package com.example.txn.api;

import com.example.txn.service.TxnStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TxnApi {
  private final TxnStore store;

  @PostMapping("/internal/payments/{id}")
  public ResponseEntity<Void> create(@PathVariable UUID id){
    store.create(id);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("/payments/{id}")
  public ResponseEntity<TxnStore.Txn> get(@PathVariable UUID id){
    var txn = store.get(id);
    if (txn == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok(txn);
  }

  // temporary endpoint to simulate provider success
  @PostMapping("/internal/payments/{id}/succeed")
  public ResponseEntity<Void> succeed(@PathVariable UUID id){
    store.markSucceeded(id);
    return ResponseEntity.accepted().build();
  }
}
