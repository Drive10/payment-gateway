package dev.payflow.gateway.service;

import dev.payflow.gateway.document.PaymentLink;
import dev.payflow.gateway.document.Transaction;
import dev.payflow.gateway.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentLinkService paymentLinkService;

    public TransactionService(TransactionRepository transactionRepository, PaymentLinkService paymentLinkService) {
        this.transactionRepository = transactionRepository;
        this.paymentLinkService = paymentLinkService;
    }

    public Transaction processPayment(String paymentLinkUuid, String paymentMethod, boolean testMode) {
        PaymentLink link = paymentLinkService.findByUuid(paymentLinkUuid);
        
        if (!paymentLinkService.validate(paymentLinkUuid)) {
            throw new RuntimeException("Invalid or expired payment link");
        }

        Transaction tx = new Transaction();
        tx.setPaymentLinkId(link.getId());
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setAmount(link.getAmount());
        tx.setCurrency(link.getCurrency());
        tx.setPaymentMethod(Transaction.PaymentMethod.valueOf(paymentMethod));
        tx.setStatus(Transaction.TransactionStatus.PENDING);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());

        if (testMode) {
            tx.setStatus(Transaction.TransactionStatus.SUCCESS);
            tx.setGatewayResponse("TEST_MODE: Payment simulated successfully");
            paymentLinkService.updateStatus(link.getUuid(), PaymentLink.LinkStatus.PAID);
        } else {
            tx.setStatus(Transaction.TransactionStatus.PROCESSING);
            tx.setGatewayResponse("LIVE_MODE: Payment processing");
        }

        return transactionRepository.save(tx);
    }

    public Transaction findByTransactionId(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
