-- Seed dummy data for development testing
-- Version: 2
-- Description: Seed demo merchants and users to skip onboarding

SET search_path TO auth_schema;

-- Insert roles (already in V1, but ensuring they exist)
INSERT INTO roles (id, name) VALUES
('00000000-0000-0000-0000-000000000001', 'ROLE_ADMIN'),
('00000000-0000-0000-0000-000000000002', 'ROLE_MERCHANT'),
('00000000-0000-0000-0000-000000000003', 'ROLE_CUSTOMER')
ON CONFLICT (id) DO NOTHING;

-- Insert demo users (password: Password123)
INSERT INTO users (id, email, password, first_name, last_name, enabled, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'admin@payflow.dev', '$2a$10$EqKcp1WFKVQISheBR4J5IOL5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Admin', 'User', true, NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'customer@test.com', '$2a$10$EqKcp1WFKVQISheBR4J5IOL5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Test', 'Customer', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id) VALUES
('11111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000000001'),
('22222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000000003')
ON CONFLICT DO NOTHING;

-- Insert demo merchants (API key: test_merchant_api_key_123)
INSERT INTO merchants (id, email, password, business_name, api_key, webhook_url, status, created_at, updated_at) VALUES
('33333333-3333-3333-3333-333333333333', 'merchant@test.com', '$2a$10$EqKcp1WFKVQISheBR4J5IOL5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Demo Electronics Store', 'test_merchant_api_key_123', 'https://test.example.com/webhook', 'ACTIVE', NOW(), NOW()),
('44444444-4444-4444-4444-444444444444', 'merchant2@test.com', '$2a$10$EqKcp1WFKVQISheBR4J5IOL5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Demo Fashion Boutique', 'test_merchant_api_key_456', 'https://fashion.test.com/webhook', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Insert refresh tokens
INSERT INTO refresh_tokens (id, token, user_id, merchant_id, expires_at, revoked) VALUES
('55555555-5555-5555-5555-555555555555', 'dev_refresh_token_123', '11111111-1111-1111-1111-111111111111', NULL, NOW() + INTERVAL '90 days', false),
('66666666-6666-6666-6666-666666666666', 'dev_refresh_token_456', NULL, '33333333-3333-3333-3333-333333333333', NOW() + INTERVAL '90 days', false)
ON CONFLICT (id) DO NOTHING;

SET search_path TO public;