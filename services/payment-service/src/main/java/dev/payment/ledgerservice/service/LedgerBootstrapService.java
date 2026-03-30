package dev.payment.ledgerservice.service;

import dev.payment.ledgerservice.domain.AccountType;
import dev.payment.ledgerservice.dto.request.CreateAccountRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class LedgerBootstrapService {

    private final LedgerService ledgerService;

    public LedgerBootstrapService(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostConstruct
    void ensureDefaultAccounts() {
        ledgerService.createAccount(new CreateAccountRequest("CASH_ASSET", "Cash Asset", AccountType.ASSET));
        ledgerService.createAccount(new CreateAccountRequest("CUSTOMER_FUNDS_LIABILITY", "Customer Funds Liability", AccountType.LIABILITY));
    }
}
