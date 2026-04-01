alter table if exists ledger_accounts
    add column if not exists account_type varchar(64);

update ledger_accounts
set account_type = coalesce(account_type, nullif(type, ''))
where account_type is null;

alter table if exists ledger_accounts
    add column if not exists merchant_id uuid;

alter table if exists ledger_accounts
    add column if not exists currency varchar(8) not null default 'INR';

alter table if exists ledger_accounts
    add column if not exists balance numeric(19, 4) not null default 0;

alter table if exists ledger_accounts
    add column if not exists available_balance numeric(19, 4) not null default 0;

alter table if exists ledger_accounts
    add column if not exists locked_balance numeric(19, 4) not null default 0;

update ledger_accounts
set balance = coalesce(balance, 0),
    available_balance = coalesce(available_balance, balance, 0),
    locked_balance = coalesce(locked_balance, 0),
    currency = coalesce(nullif(currency, ''), 'INR'),
    account_type = coalesce(account_type, 'CUSTOMER_ESCROW');

alter table if exists ledger_accounts
    alter column balance type numeric(19, 4);

alter table if exists ledger_accounts
    alter column available_balance type numeric(19, 4);

alter table if exists ledger_accounts
    alter column locked_balance type numeric(19, 4);

alter table if exists ledger_accounts
    alter column account_type set not null;

create index if not exists idx_ledger_accounts_merchant on ledger_accounts(merchant_id);
create index if not exists idx_ledger_accounts_type on ledger_accounts(account_type);

create table if not exists ledger_entries (
    id uuid primary key,
    entry_id varchar(64) not null unique,
    transaction_id varchar(64) not null,
    transaction_type varchar(64) not null,
    account_id uuid not null,
    entry_type varchar(32) not null,
    amount numeric(19, 4) not null,
    balance_after numeric(19, 4) not null,
    reference_type varchar(64),
    reference_id uuid,
    description varchar(500),
    metadata jsonb,
    created_at timestamp with time zone not null
);

create index if not exists idx_ledger_entries_entry_id on ledger_entries(entry_id);
create index if not exists idx_ledger_entries_transaction on ledger_entries(transaction_id);
create index if not exists idx_ledger_entries_account on ledger_entries(account_id);
create index if not exists idx_ledger_entries_created on ledger_entries(created_at);
