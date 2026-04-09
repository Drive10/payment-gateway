-- Payment Service Baseline
-- V1__baseline.sql

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID,
    provider_order_id VARCHAR(64) NOT NULL UNIQUE,
    provider_payment_id VARCHAR(64) UNIQUE,
    idempotency_key VARCHAR(120) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    refunded_amount DECIMAL(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    provider VARCHAR(32) NOT NULL,
    method VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    transaction_mode VARCHAR(16) NOT NULL DEFAULT 'TEST',
    simulated BOOLEAN NOT NULL DEFAULT FALSE,
    provider_signature VARCHAR(255),
    checkout_url VARCHAR(255) NOT NULL,
    notes VARCHAR(255),
    merchant_id UUID NOT NULL,
    platform_fee DECIMAL(19,2) NOT NULL DEFAULT 0,
    gateway_fee DECIMAL(19,2) NOT NULL DEFAULT 0,
    pricing_tier VARCHAR(32) DEFAULT 'STANDARD',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_provider_order_id ON payments(provider_order_id);
CREATE INDEX idx_payments_idempotency_key ON payments(idempotency_key);

-- Payment Methods table
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    last_four VARCHAR(4),
    provider_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_methods_payment_id ON payment_methods(payment_id);

-- Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action VARCHAR(80) NOT NULL,
    actor VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(80) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_logs_resource_id ON audit_logs(resource_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);