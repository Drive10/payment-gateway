-- Initialize PayFlow database
-- This script seeds initial data for development

USE payment_gateway;

-- Insert default roles (if not exists)
INSERT INTO roles (id, name) VALUES 
    (1, 'ROLE_USER'),
    (2, 'ROLE_ADMIN'),
    (3, 'ROLE_MERCHANT')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Insert a demo admin user (password: admin123)
INSERT INTO users (id, email, password_hash, first_name, last_name, created_at, updated_at) 
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'admin@payflow.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye/U7Q6ZnSqHqh/3fNqYZwBKJ8L.z8f.S',
    'Admin',
    'User',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE email = email;

-- Assign admin role
INSERT INTO user_roles (user_id, role_id)
SELECT '11111111-1111-1111-1111-111111111111', 2
WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = '11111111-1111-1111-1111-111111111111' AND role_id = 2);

-- Insert a demo merchant
INSERT INTO merchants (id, name, email, status, created_at, updated_at)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Demo Store',
    'merchant@payflow.local',
    'ACTIVE',
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE name = name;

-- Insert demo API key for merchant
INSERT INTO api_keys (id, merchant_id, key_hash, name, active, created_at)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    '$2a$10$abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJ',
    'Demo API Key',
    true,
    NOW()
)
ON DUPLICATE KEY UPDATE active = true;

-- Insert demo feature flags
INSERT INTO feature_flags (id, name, enabled, description)
VALUES 
    (1, 'PAYMENT_UPI_ENABLED', true, 'Enable UPI payments'),
    (2, 'PAYMENT_CARD_ENABLED', true, 'Enable card payments'),
    (3, 'NOTIFICATION_EMAIL_ENABLED', true, 'Enable email notifications'),
    (4, 'NOTIFICATION_SMS_ENABLED', false, 'Enable SMS notifications (requires provider)'),
    (5, 'ANALYTICS_ENABLED', true, 'Enable analytics tracking'),
    (6, 'DEBUG_MODE', true, 'Enable debug logging')
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

SELECT 'Database initialization complete!' as status;