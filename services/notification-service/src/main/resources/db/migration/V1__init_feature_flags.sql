CREATE TABLE IF NOT EXISTS feature_flags (
    id SERIAL PRIMARY KEY,
    key VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT false,
    rollout_percentage INTEGER DEFAULT 0,
    user_ids TEXT[],
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feature_flags_key ON feature_flags(key);
CREATE INDEX idx_feature_flags_enabled ON feature_flags(enabled);

INSERT INTO feature_flags (key, description, enabled, rollout_percentage) VALUES
    ('new_payment_flow', 'Enable new payment processing flow', false, 0),
    ('beta_features', 'Enable beta features for testing', false, 10),
    ('enhanced_reporting', 'Enable enhanced analytics and reporting', true, 100),
    ('two_factor_auth', 'Require 2FA for all transactions', false, 0)
ON CONFLICT (key) DO NOTHING;
