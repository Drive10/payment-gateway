package com.example.txn.service;

import com.example.txn.model.Transaction;
import com.example.txn.repo.TransactionRepo;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TxnService {
    private final TransactionRepo repo;

    public TxnService(TransactionRepo repo) { this.repo = repo; }

    public void create(UUID id, UUID userId, BigDecimal amount, String currency) {
        var now = OffsetDateTime.now();
        var t = new Transaction(id, userId, amount, currency, "PENDING", null, now, now);
        repo.save(t);
    }

    public Optional<Transaction> get(UUID id) { return repo.findById(id); }

    public void succeed(UUID id) {
        repo.findById(id).ifPresent(t -> {
            t.setStatus("SUCCEEDED");
            t.setUpdatedAt(OffsetDateTime.now());
            repo.save(t);
        });
    }
}
