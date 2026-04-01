-- Rebuild ledger schema to match entity model
-- V201/V202 created a different schema that doesn't match the JPA entities

-- Drop old schema
drop table if exists journal_entries cascade;
drop table if exists ledger_accounts cascade;

-- Create ledger_accounts matching LedgerAccount entity
create table ledger_accounts (
    id uuid primary key default gen_random_uuid(),
    account_type varchar(32) not null,
    merchant_id uuid,
    currency varchar(3) not null,
    balance numeric(19, 4) not null default 0,
    available_balance numeric(19, 4) not null default 0,
    locked_balance numeric(19, 4) not null default 0,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone
);

create index idx_ledger_accounts_merchant on ledger_accounts(merchant_id);
create index idx_ledger_accounts_type on ledger_accounts(account_type);

-- Create ledger_entries matching LedgerEntry entity
create table ledger_entries (
    id uuid primary key default gen_random_uuid(),
    entry_id uuid not null unique,
    transaction_id uuid not null,
    transaction_type varchar(32) not null,
    account_id uuid not null,
    entry_type varchar(16) not null,
    amount numeric(19, 4) not null,
    balance_after numeric(19, 4),
    reference_type varchar(32) not null,
    reference_id varchar(64) not null,
    description varchar(255),
    metadata jsonb,
    created_at timestamp with time zone not null default now()
);

create index idx_ledger_entries_transaction on ledger_entries(transaction_id);
create index idx_ledger_entries_account on ledger_entries(account_id);
create index idx_ledger_entries_reference on ledger_entries(reference_type, reference_id);
