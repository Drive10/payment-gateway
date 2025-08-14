package dev.payment.paymentservice.service;

import dev.payment.paymentservice.entity.TransactionEntity;
import dev.payment.paymentservice.repository.TransactionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@Profile("dev")
public class LocalBankSimulator {
    private final TransactionRepository txRepo;

    public LocalBankSimulator(TransactionRepository txRepo) {
        this.txRepo = txRepo;
    }

    public boolean setStatus(String transactionId, String status) {
        Optional<TransactionEntity> opt = txRepo.findByTransactionId(transactionId);
        if (opt.isEmpty()) return false;
        TransactionEntity t = opt.get();
        t.setStatus(status);
        t.setUpdatedAt(OffsetDateTime.now());
        txRepo.save(t);
        return true;
    }
}
