package com.example.txn.repo;

import com.example.txn.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TransactionRepo extends JpaRepository<Transaction, UUID> { }
