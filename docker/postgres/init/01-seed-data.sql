-- Seed data for authdb (auth-service)
-- Users
INSERT INTO users (id, email, password_hash, name, role, status, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'admin@payment.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqE3p0Z7V8V0Z9qo8uLOickgx2ZMR', 'Admin User', 'ADMIN', 'ACTIVE', NOW(), NOW()),
    ('22222222-2222-2222-2222-222222222222', 'demo@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqE3p0Z7V8V0Z9qo8uLOickgx2ZMR', 'Demo User', 'USER', 'ACTIVE', NOW(), NOW()),
    ('33333333-3333-3333-3333-333333333333', 'merchant@store.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqE3p0Z7V8V0Z9qo8uLOickgx2ZMR', 'Store Merchant', 'MERCHANT', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Sessions
INSERT INTO sessions (id, user_id, refresh_token, expires_at, created_at) VALUES
    ('aaaa1111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'admin-refresh-token-xxx', NOW() + INTERVAL '7 days', NOW()),
    ('bbbb2222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'demo-refresh-token-xxx', NOW() + INTERVAL '7 days', NOW())
ON CONFLICT (id) DO NOTHING;

-- API Keys
INSERT INTO api_keys (id, user_id, key_prefix, key_hash, name, permissions, rate_limit, expires_at, created_at) VALUES
    ('cccc1111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'pk_live_demo', 'hash123', 'Production Key', '["payments:write", "payments:read", "refunds:write"]', 1000, NULL, NOW())
ON CONFLICT (id) DO NOTHING;

-- Feature Flags (for featureflagsdb)
INSERT INTO feature_flags (key, description, enabled, rollout_percentage) VALUES
    ('new_payment_flow', 'Enable new payment processing flow', false, 0),
    ('enhanced_reporting', 'Enable enhanced analytics and reporting', true, 100),
    ('two_factor_auth', 'Require 2FA for all transactions', false, 0),
    ('beta_features', 'Enable beta features for testing', false, 10)
ON CONFLICT (key) DO NOTHING;
