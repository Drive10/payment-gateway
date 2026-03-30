package dev.payment.ledgerservice.service;

import dev.payment.common.events.PaymentEventMessage;
import dev.payment.ledgerservice.dto.request.PostJournalRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class LedgerEventPostingService {

    private final LedgerService ledgerService;
    private final String cashAccountCode;
    private final String customerFundsAccountCode;

    public LedgerEventPostingService(
            LedgerService ledgerService,
            @Value("${application.ledger.accounts.cash}") String cashAccountCode,
            @Value("${application.ledger.accounts.customer-funds}") String customerFundsAccountCode
    ) {
        this.ledgerService = ledgerService;
        this.cashAccountCode = cashAccountCode;
        this.customerFundsAccountCode = customerFundsAccountCode;
    }

    public void postCapture(PaymentEventMessage message) {
        ledgerService.postJournal(new PostJournalRequest(
                "capture:" + message.paymentId(),
                cashAccountCode,
                customerFundsAccountCode,
                message.amount(),
                "Payment capture for " + message.orderReference()
        ));
    }

    public void postRefund(PaymentEventMessage message) {
        Map<String, String> metadata = message.metadata();
        String refundReference = metadata == null ? null : metadata.get("refundReference");
        String refundAmount = metadata == null ? null : metadata.get("refundAmount");
        if (refundReference == null || refundReference.isBlank() || refundAmount == null || refundAmount.isBlank()) {
            throw new IllegalStateException("Refund event is missing refundReference or refundAmount metadata");
        }

        ledgerService.postJournal(new PostJournalRequest(
                "refund:" + refundReference,
                customerFundsAccountCode,
                cashAccountCode,
                new BigDecimal(refundAmount),
                "Refund for " + message.orderReference()
        ));
    }
}
