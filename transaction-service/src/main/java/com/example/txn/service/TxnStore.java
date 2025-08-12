package com.example.txn.service;
import org.springframework.stereotype.Component;
import java.util.Map; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
@Component
public class TxnStore {
  public record Txn(UUID id, String status) {}
  private final Map<UUID, Txn> store = new ConcurrentHashMap<>();
  public void create(UUID id){ store.put(id, new Txn(id, "PENDING")); }
  public Txn get(UUID id){ return store.get(id); }
  public void markSucceeded(UUID id){ store.put(id, new Txn(id, "SUCCEEDED")); }
}
