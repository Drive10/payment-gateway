-- Payment Service Canonical Schema
-- Version: 1
-- Description: Create canonical tables for payment domain

CREATE SCHEMA IF NOT EXISTS payment_schema;
SET search_path TO payment_schema;

-- Payment Intent (main entity)
CREATE TABLE IF NOT EXISTS payment_intent (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    idempotency_key VARCHAR(255) UNIQUE,
    merchant_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    metadata TEXT,
    client_secret VARCHAR(500),
    checkout_url VARCHAR(500),
    provider VARCHAR(100),
    provider_order_id VARCHAR(255),
    provider_payment_id VARCHAR(255),
    provider_reference VARCHAR(255),
    provider_signature TEXT,
    failure_reason TEXT,
    method VARCHAR(50),
    upi_id VARCHAR(255),
    upi_link VARCHAR(500),
    notes TEXT,
    pricing_tier VARCHAR(100),
    platform_fee DECIMAL(19,4) DEFAULT 0,
    gateway_fee DECIMAL(19,4) DEFAULT 0,
    refunded_amount DECIMAL(19,4) DEFAULT 0,
    metadata_json JSONB
);

-- Payment Attempt (authorization attempts)
CREATE TABLE IF NOT EXISTS payment_attempt (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    payment_intent_id UUID NOT NULL REFERENCES payment_intent(id),
    attempt_number INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    provider_reference VARCHAR(255),
    provider_order_id VARCHAR(255),
    provider_payment_id VARCHAR(255),
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    metadata_json JSONB
);

-- Authorization (capture of funds)
CREATE TABLE IF NOT EXISTS authorization (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    payment_intent_id UUID NOT NULL REFERENCES payment_intent(id),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider VARCHAR(100),
    provider_authorization_id VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE,
    captured_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Capture (funds captured)
CREATE TABLE IF NOT EXISTS capture (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    payment_intent_id UUID NOT NULL REFERENCES payment_intent(id),
    authorization_id UUID REFERENCES authorization(id),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    provider_capture_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Refund
CREATE TABLE IF NOT EXISTS refund (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    payment_intent_id UUID NOT NULL REFERENCES payment_intent(id),
    capture_id UUID REFERENCES capture(id),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT,
    refund_reference VARCHAR(255),
    provider_refund_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Idempotency Keys (unique constraint enforcement)
CREATE TABLE IF NOT EXISTS idempotency_key (
    id UUID PRIMARY KEY,
    key VARCHAR(255) NOT NULL UNIQUE,
    payment_intent_id UUID REFERENCES payment_intent(id),
    endpoint VARCHAR(255) NOT NULL,
    response_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE
);

-- Outbox for reliable event publishing
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMP WITH TIME ZONE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_payment_intent_merchant ON payment_intent(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payment_intent_status ON payment_intent(status);
CREATE INDEX IF NOT EXISTS idx_payment_intent_idempotency ON payment_intent(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_payment_intent_created ON payment_intent(created_at);
CREATE INDEX IF NOT EXISTS idx_payment_attempt_intent ON payment_attempt(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_authorization_intent ON authorization(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_capture_intent ON capture(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_refund_intent ON refund(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_key(key) WHERE expires_at > CURRENT_TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox(processed_at) WHERE processed_at IS NULL;

SET search_path TO public;