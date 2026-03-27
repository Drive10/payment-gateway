package dev.payment.ledgerservice.service;

import dev.payment.ledgerservice.domain.AccountType;
import dev.payment.ledgerservice.domain.JournalEntry;
import dev.payment.ledgerservice.domain.LedgerAccount;
import dev.payment.ledgerservice.dto.request.CreateAccountRequest;
import dev.payment.ledgerservice.dto.request.PostJournalRequest;
import dev.payment.ledgerservice.exception.ApiException;
import dev.payment.ledgerservice.repository.JournalEntryRepository;
import dev.payment.ledgerservice.repository.LedgerAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerAccountRepository ledgerAccountRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(ledgerAccountRepository, journalEntryRepository);
    }

    @Test
    void shouldTreatCreateAccountAsIdempotentForExistingCode() {
        LedgerAccount existing = account("cash_asset", "Cash Asset", AccountType.ASSET);
        when(ledgerAccountRepository.findByAccountCode("CASH_ASSET")).thenReturn(Optional.of(existing));
        when(journalEntryRepository.sumDebits("cash_asset")).thenReturn(BigDecimal.ZERO);
        when(journalEntryRepository.sumCredits("cash_asset")).thenReturn(BigDecimal.ZERO);

        var response = ledgerService.createAccount(new CreateAccountRequest("cash_asset", "Cash Asset", AccountType.ASSET));

        assertThat(response.accountCode()).isEqualTo("cash_asset");
        verify(ledgerAccountRepository, never()).save(any(LedgerAccount.class));
    }

    @Test
    void shouldRejectJournalWhenDebitAndCreditAccountsMatch() {
        assertThatThrownBy(() -> ledgerService.postJournal(new PostJournalRequest(
                "journal-1001",
                "CASH_ASSET",
                "cash_asset",
                new BigDecimal("10.00"),
                "invalid pair"
        )))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> assertThat(((ApiException) exception).getCode()).isEqualTo("INVALID_LEDGER_PAIR"));
    }

    @Test
    void shouldReturnExistingJournalForDuplicateReference() {
        JournalEntry existing = new JournalEntry();
        existing.setReference("capture:pay_1001");
        existing.setDebitAccountCode("CASH_ASSET");
        existing.setCreditAccountCode("CUSTOMER_FUNDS_LIABILITY");
        existing.setAmount(new BigDecimal("2499.00"));
        existing.setNarration("capture");
        when(journalEntryRepository.findByReference("capture:pay_1001")).thenReturn(Optional.of(existing));

        var response = ledgerService.postJournal(new PostJournalRequest(
                "capture:pay_1001",
                "CASH_ASSET",
                "CUSTOMER_FUNDS_LIABILITY",
                new BigDecimal("2499.00"),
                "capture"
        ));

        assertThat(response.reference()).isEqualTo("capture:pay_1001");
        verify(journalEntryRepository, never()).save(any(JournalEntry.class));
    }

    @Test
    void shouldDeriveAccountBalanceFromJournalEntries() {
        LedgerAccount asset = account("CASH_ASSET", "Cash Asset", AccountType.ASSET);
        when(ledgerAccountRepository.findAll()).thenReturn(List.of(asset));
        when(journalEntryRepository.sumDebits("CASH_ASSET")).thenReturn(new BigDecimal("2499.00"));
        when(journalEntryRepository.sumCredits("CASH_ASSET")).thenReturn(new BigDecimal("499.00"));

        var responses = ledgerService.getAccounts();

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().balance()).isEqualByComparingTo("2000.00");
    }

    @Test
    void shouldCreateJournalForDistinctDebitAndCreditAccounts() {
        LedgerAccount debit = account("CASH_ASSET", "Cash Asset", AccountType.ASSET);
        LedgerAccount credit = account("CUSTOMER_FUNDS_LIABILITY", "Customer Funds", AccountType.LIABILITY);
        when(journalEntryRepository.findByReference("capture:pay_2001")).thenReturn(Optional.empty());
        when(ledgerAccountRepository.findByAccountCode("CASH_ASSET")).thenReturn(Optional.of(debit));
        when(ledgerAccountRepository.findByAccountCode("CUSTOMER_FUNDS_LIABILITY")).thenReturn(Optional.of(credit));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = ledgerService.postJournal(new PostJournalRequest(
                "capture:pay_2001",
                "cash_asset",
                "customer_funds_liability",
                new BigDecimal("2499.00"),
                "captured payment"
        ));

        assertThat(response.debitAccountCode()).isEqualTo("CASH_ASSET");
        assertThat(response.creditAccountCode()).isEqualTo("CUSTOMER_FUNDS_LIABILITY");
        assertThat(response.amount()).isEqualByComparingTo("2499.00");
    }

    private LedgerAccount account(String code, String name, AccountType type) {
        LedgerAccount account = new LedgerAccount();
        account.setAccountCode(code);
        account.setAccountName(name);
        account.setType(type);
        return account;
    }
}
