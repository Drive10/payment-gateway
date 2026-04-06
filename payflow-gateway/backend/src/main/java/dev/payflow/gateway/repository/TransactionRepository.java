package dev.payflow.gateway.repository;

import dev.payflow.gateway.document.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);
    List<Transaction> findByPaymentLinkId(String paymentLinkId);
    List<Transaction> findByOrderId(String orderId);
}
