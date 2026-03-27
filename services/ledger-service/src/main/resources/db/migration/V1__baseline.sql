create table ledger_accounts (
    id uuid primary key,
    account_code varchar(64) not null unique,
    account_name varchar(120) not null,
    type varchar(32) not null,
    balance numeric(19, 2) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table journal_entries (
    id uuid primary key,
    reference varchar(64) not null unique,
    debit_account_code varchar(64) not null,
    credit_account_code varchar(64) not null,
    amount numeric(19, 2) not null,
    narration varchar(255) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_journal_entries_debit on journal_entries(debit_account_code);
create index idx_journal_entries_credit on journal_entries(credit_account_code);
