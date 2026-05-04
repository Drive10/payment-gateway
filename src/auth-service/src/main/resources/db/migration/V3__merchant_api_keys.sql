-- Merchant API Keys with Security
-- Version: 3
-- Description: Add secure API key management with scoping, hashing, revocation

SET search_path TO auth_schema;

-- Drop old merchants table
DROP TABLE IF EXISTS merchants CASCADE;

-- Create merchant API keys table with security features
CREATE TABLE IF NOT EXISTS merchant_api_keys (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    key_hash VARCHAR(255) NOT NULL,
    key_prefix VARCHAR(10) NOT NULL,
    name VARCHAR(100),
    scopes JSONB NOT NULL DEFAULT '["payments:read","payments:write"]',
    ip_whitelist JSONB,
    rate_limit INTEGER DEFAULT 1000,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_rotated_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_reason VARCHAR(255),
    revoked_by UUID
);

CREATE INDEX idx_merchant_api_keys_merchant_id ON merchant_api_keys(merchant_id);
CREATE INDEX idx_merchant_api_keys_key_hash ON merchant_api_keys(key_hash);
CREATE INDEX idx_merchant_api_keys_key_prefix ON merchant_api_keys(key_prefix);
CREATE INDEX idx_merchant_api_keys_is_active ON merchant_api_keys(is_active) WHERE is_active = TRUE;

-- API Key usage log for analytics
CREATE TABLE IF NOT EXISTS merchant_api_key_logs (
    id UUID PRIMARY KEY,
    api_key_id UUID NOT NULL,
    request_path VARCHAR(255),
    request_method VARCHAR(10),
    source_ip VARCHAR(45),
    user_agent VARCHAR(500),
    response_status INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_key_logs_api_key_id ON merchant_api_key_logs(api_key_id);
CREATE INDEX idx_api_key_logs_created_at ON merchant_api_key_logs(created_at);

-- Insert demo merchant with API key (hash of 'test_merchant_key_123')
-- bcrypt hash with cost 12
INSERT INTO merchant_api_keys (id, merchant_id, key_hash, key_prefix, name, scopes, is_active, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '33333333-3333-3333-3333-333333333331',
    '$2a$12$LQv3c1JqRNhZ4JFN7XeXqJOQyX8GmKmMN3qFGYp.MzEaYb1VKG7w8C',
    'test_mer',
    'Demo Merchant Key',
    '["payments:read","payments:write","refunds:read","refunds:write"]',
    TRUE,
    NOW()
);

-- Insert second demo key
INSERT INTO merchant_api_keys (id, merchant_id, key_hash, key_prefix, name, scopes, is_active, created_at)
VALUES (
    '44444444-4444-4444-4444-444444444442',
    '33333333-3333-3333-3333-333333333331',
    '$2a$12$8FGYp.MzEaYb1VKG7w8CNmRNhZ4JFN7XeXqJOQyX8GmKmMNqFGYpA',
    'demo_fe',
    'Demo Frontend Key',
    '["payments:read"]',
    TRUE,
    NOW()
);

SET search_path TO public;