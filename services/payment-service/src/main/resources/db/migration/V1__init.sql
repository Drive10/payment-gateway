-- Flyway baseline schema
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) UNIQUE NOT NULL,
    provider VARCHAR(64) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(16) NOT NULL,
    status VARCHAR(32),
    idempotency_key VARCHAR(64),
    customer_email VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_txn_id ON transactions(transaction_id);
CREATE INDEX IF NOT EXISTS idx_idempotency ON transactions(idempotency_key);

CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    transaction_id VARCHAR(255) NOT NULL,
    amount_minor BIGINT NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(32),
    created_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ref_txn ON refunds(transaction_id);

CREATE TABLE IF NOT EXISTS merchants (
    id BIGSERIAL PRIMARY KEY,
    merchant_key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    webhook_url TEXT
);
