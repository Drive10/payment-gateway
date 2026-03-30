alter table ledger_accounts
    drop column if exists balance;

alter table journal_entries
    add constraint chk_journal_entries_positive_amount check (amount > 0);

alter table journal_entries
    add constraint chk_journal_entries_distinct_accounts check (debit_account_code <> credit_account_code);
