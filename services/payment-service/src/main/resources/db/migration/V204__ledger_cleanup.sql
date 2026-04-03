-- Rebuild ledger schema to align with LedgerAccount.java
-- This migration recreates ledger schema to match the JPA entity model
DROP TABLE IF EXISTS ledger_entries CASCADE;
DROP TABLE IF EXISTS ledger_accounts CASCADE;

CREATE TABLE ledger_accounts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_type VARCHAR(32) NOT NULL,
  merchant_id UUID,
  currency VARCHAR(3) NOT NULL,
  balance NUMERIC(19,4) NOT NULL DEFAULT 0,
  available_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
  locked_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ
);

CREATE INDEX idx_ledger_accounts_merchant ON ledger_accounts(merchant_id);
CREATE INDEX idx_ledger_accounts_type ON ledger_accounts(account_type);

CREATE TABLE ledger_entries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  entry_id UUID NOT NULL UNIQUE,
  transaction_id UUID NOT NULL,
  transaction_type VARCHAR(32) NOT NULL,
  account_id UUID NOT NULL,
  entry_type VARCHAR(16) NOT NULL,
  amount NUMERIC(19,4) NOT NULL,
  balance_after NUMERIC(19,4),
  reference_type VARCHAR(32) NOT NULL,
  reference_id VARCHAR(64) NOT NULL,
  description VARCHAR(255),
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_transaction ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_reference ON ledger_entries(reference_type, reference_id);
