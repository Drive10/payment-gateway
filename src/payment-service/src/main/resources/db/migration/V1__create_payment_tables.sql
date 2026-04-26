-- Payment Service Schema
-- Version: 1
-- Description: Create initial tables for payment service

CREATE SCHEMA IF NOT EXISTS payment_schema;
SET search_path TO payment_schema;

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id VARCHAR(255),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    payment_method VARCHAR(100),
    provider_reference VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    metadata TEXT,
    checkout_url VARCHAR(500),
    idempotency_key VARCHAR(255),
    provider VARCHAR(100),
    provider_order_id VARCHAR(255),
    provider_payment_id VARCHAR(255),
    provider_signature TEXT,
    simulated BOOLEAN NOT NULL DEFAULT FALSE,
    transaction_mode VARCHAR(50),
    method VARCHAR(50),
    upi_id VARCHAR(255),
    upi_link VARCHAR(500),
    notes TEXT,
    pricing_tier VARCHAR(100),
    platform_fee DECIMAL(19,4) DEFAULT 0,
    gateway_fee DECIMAL(19,4) DEFAULT 0,
    refund_amount DECIMAL(19,4) DEFAULT 0
);

-- Refunds table
CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL,
    refund_id VARCHAR(255) NOT NULL UNIQUE,
    amount DECIMAL(19,4) NOT NULL,
    refunded_amount DECIMAL(19,4) DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Outbox table for reliable event publishing
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_merchant_id ON payments(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_idempotency_key ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_outbox_processed_at ON outbox(processed_at) WHERE processed_at IS NULL;

SET search_path TO public;