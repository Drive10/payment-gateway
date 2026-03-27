package dev.payment.ledgerservice.service;

import dev.payment.ledgerservice.domain.JournalEntry;
import dev.payment.ledgerservice.domain.LedgerAccount;
import dev.payment.ledgerservice.dto.request.CreateAccountRequest;
import dev.payment.ledgerservice.dto.request.PostJournalRequest;
import dev.payment.ledgerservice.dto.response.AccountResponse;
import dev.payment.ledgerservice.dto.response.JournalResponse;
import dev.payment.ledgerservice.exception.ApiException;
import dev.payment.ledgerservice.repository.JournalEntryRepository;
import dev.payment.ledgerservice.repository.LedgerAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {
    private final LedgerAccountRepository ledgerAccountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public LedgerService(LedgerAccountRepository ledgerAccountRepository, JournalEntryRepository journalEntryRepository) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalEntryRepository = journalEntryRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        LedgerAccount existing = ledgerAccountRepository.findByAccountCode(request.accountCode().toUpperCase()).orElse(null);
        if (existing != null) {
            return toAccount(existing);
        }

        LedgerAccount account = new LedgerAccount();
        account.setAccountCode(request.accountCode().toUpperCase());
        account.setAccountName(request.accountName());
        account.setType(request.type());
        ledgerAccountRepository.save(account);
        return toAccount(account);
    }

    @Transactional
    public JournalResponse postJournal(PostJournalRequest request) {
        JournalEntry existing = journalEntryRepository.findByReference(request.reference()).orElse(null);
        if (existing != null) {
            return toJournal(existing);
        }

        if (request.debitAccountCode().equalsIgnoreCase(request.creditAccountCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LEDGER_PAIR", "Debit and credit accounts must be different");
        }

        LedgerAccount debit = ledgerAccountRepository.findByAccountCode(request.debitAccountCode().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEBIT_ACCOUNT_NOT_FOUND", "Debit account not found"));
        LedgerAccount credit = ledgerAccountRepository.findByAccountCode(request.creditAccountCode().toUpperCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CREDIT_ACCOUNT_NOT_FOUND", "Credit account not found"));
        JournalEntry entry = new JournalEntry();
        entry.setReference(request.reference());
        entry.setDebitAccountCode(debit.getAccountCode());
        entry.setCreditAccountCode(credit.getAccountCode());
        entry.setAmount(request.amount());
        entry.setNarration(request.narration());
        journalEntryRepository.save(entry);
        return toJournal(entry);
    }

    public List<AccountResponse> getAccounts() {
        return ledgerAccountRepository.findAll().stream().map(this::toAccount).toList();
    }

    private AccountResponse toAccount(LedgerAccount account) {
        BigDecimal balance = journalEntryRepository.sumDebits(account.getAccountCode())
                .subtract(journalEntryRepository.sumCredits(account.getAccountCode()));
        return new AccountResponse(account.getId(), account.getAccountCode(), account.getAccountName(), account.getType().name(), balance, account.getCreatedAt());
    }

    private JournalResponse toJournal(JournalEntry entry) {
        return new JournalResponse(entry.getId(), entry.getReference(), entry.getDebitAccountCode(), entry.getCreditAccountCode(), entry.getAmount(), entry.getNarration(), entry.getCreatedAt());
    }
}
