-- Initialize schemas for each service
-- This script runs on first startup of the PostgreSQL container

-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS analytics_schema;
CREATE SCHEMA IF NOT EXISTS audit_schema;

-- payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id VARCHAR(255),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    payment_method VARCHAR(50),
    provider VARCHAR(100),
    provider_payment_id VARCHAR(255),
    platform_fee NUMERIC(19, 4),
    gateway_fee NUMERIC(19, 4),
    captured_amount NUMERIC(19, 4),
    refunded_amount NUMERIC(19, 4),
    failure_reason TEXT,
    checkout_url TEXT,
    client_secret VARCHAR(500),
    requires_action BOOLEAN DEFAULT FALSE,
    next_action VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    captured_at TIMESTAMP,
    capture_initiated_at TIMESTAMP,
    capture_idempotency_key VARCHAR(255),
    expires_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);

-- ledger_entries table for double-entry bookkeeping
CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id VARCHAR(255) NOT NULL,
    refund_id VARCHAR(255),
    entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    reference VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ledger_payment_id ON ledger_entries(payment_id);
CREATE INDEX IF NOT EXISTS idx_ledger_reference ON ledger_entries(reference);

-- outbox table for event publishing
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_outbox_processed ON outbox(processed_at) WHERE processed_at IS NULL;

-- webhook_inbox_events table
CREATE TABLE IF NOT EXISTS webhook_inbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    webhook_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100),
    payment_id VARCHAR(255),
    payload TEXT,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

DO $$
BEGIN
    RAISE NOTICE 'Service schemas and tables initialized successfully';
END $$;