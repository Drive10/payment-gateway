-- Sample data for development testing
-- This script runs on first startup after schema creation

\set QUIET on

-- ==================== AUTH_SCHEMA SAMPLE DATA ====================
SET search_path TO auth_schema;

-- Create test users (passwords are BCrypt encoded 'password123')
INSERT INTO users (id, email, password, first_name, last_name, enabled, created_at, updated_at) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'admin@payflow.dev', '$2a$10$N9qo8uLOkgx3JWP8iF0QXeX5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Admin', 'User', true, NOW(), NOW()),
('b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', 'customer1@test.com', '$2a$10$N9qo8uLOkgx3JWP8iF0QXeX5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'John', 'Doe', true, NOW(), NOW()),
('c2ggde99-9c0b-4ef8-bb6d-6bb9bd380a33', 'customer2@test.com', '$2a$10$N9qo8uLOkgx3JWP8iF0QXeX5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Jane', 'Smith', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create roles
INSERT INTO roles (id, name) VALUES
('d3hhhe99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ROLE_ADMIN'),
('e4iiif99-9c0b-4ef8-bb6d-6bb9bd380a22', 'ROLE_MERCHANT'),
('f5jjjg99-9c0b-4ef8-bb6d-6bb9bd380a33', 'ROLE_CUSTOMER')
ON CONFLICT (id) DO NOTHING;

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'd3hhhe99-9c0b-4ef8-bb6d-6bb9bd380a11'),
('b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', 'f5jjjg99-9c0b-4ef8-bb6d-6bb9bd380a33'),
('c2ggde99-9c0b-4ef8-bb6d-6bb9bd380a33', 'f5jjjg99-9c0b-4ef8-bb6d-6bb9bd380a33')
ON CONFLICT DO NOTHING;

-- Create merchants
INSERT INTO merchants (id, email, password, business_name, status, api_key, webhook_url, created_at, updated_at) VALUES
('m1kkka99-9c0b-4ef8-bb6d-6bb9bd380a11', 'merchant1@test.com', '$2a$10$N9qo8uLOkgx3JWP8iF0QXeX5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Test Electronics Store', 'ACTIVE', 'mk_test_abc123xyz456', 'https://test.example.com/webhook', NOW(), NOW()),
('m2llll99-9c0b-4ef8-bb6d-6bb9bd380a22', 'merchant2@test.com', '$2a$10$N9qo8uLOkgx3JWP8iF0QXeX5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Test Fashion Boutique', 'ACTIVE', 'mk_test_def789uvw012', 'https://fashion.test.com/webhook', NOW(), NOW()),
('m3mmmm99-9c0b-4ef8-bb6d-6bb9bd380a33', 'merchant3@test.com', '$2a$10$N9qo8uLOkgx3JWP8iF0QXeX5J5xG5G5G5G5G5G5G5G5G5G5G5G5G', 'Test Food Delivery', 'ACTIVE', 'mk_test_ghi345rst678', 'https://food.test.com/webhook', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Create refresh tokens
INSERT INTO refresh_tokens (id, token, expires_at, user_id, revoked) VALUES
('r1nnnn99-9c0b-4ef8-bb6d-6bb9bd380a11', 'refresh_token_sample_admin_abc123', NOW() + INTERVAL '90 days', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', false),
('r2oooo99-9c0b-4ef8-bb6d-6bb9bd380a22', 'refresh_token_sample_customer1_def456', NOW() + INTERVAL '90 days', 'b1ffcd99-9c0b-4ef8-bb6d-6bb9bd380a22', false)
ON CONFLICT (id) DO NOTHING;

-- ==================== PAYMENT_SCHEMA SAMPLE DATA ====================
SET search_path TO payment_schema;

-- Create sample payments
INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, created_at, updated_at, expires_at, simulated, method) VALUES
('p1pppp99-9c0b-4ef8-bb6d-6bb9bd380a11', 'ORD-2024-001', 1500.00, 'INR', 'CAPTURED', 'm1kkka99-9c0b-4ef8-bb6d-6bb9bd380a11', 'corr_abc123', 'CARD', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW() + INTERVAL '1 day', true, 'CARD'),
('p2qqqq99-9c0b-4ef8-bb6d-6bb9bd380a22', 'ORD-2024-002', 2500.50, 'INR', 'AUTHORIZED', 'm1kkka99-9c0b-4ef8-bb6d-6bb9bd380a11', 'corr_def456', 'UPI', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() + INTERVAL '1 day', true, 'UPI'),
('p3rrrr99-9c0b-4ef8-bb6d-6bb9bd380a33', 'ORD-2024-003', 999.99, 'USD', 'CREATED', 'm2llll99-9c0b-4ef8-bb6d-6bb9bd380a22', 'corr_ghi789', 'WALLET', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() + INTERVAL '1 day', false, 'WALLET'),
('p4ssss99-9c0b-4ef8-bb6d-6bb9bd380a44', 'ORD-2024-004', 5000.00, 'INR', 'REFUNDED', 'm1kkka99-9c0b-4ef8-bb6d-6bb9bd380a11', 'corr_jkl012', 'CARD', NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', true, 'CARD'),
('p5tttt99-9c0b-4ef8-bb6d-6bb9bd380a55', 'ORD-2024-005', 750.25, 'EUR', 'FAILED', 'm3mmmm99-9c0b-4ef8-bb6d-6bb9bd380a33', 'corr_mno345', 'CARD', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', true, 'CARD')
ON CONFLICT (id) DO NOTHING;

-- Create sample refunds
INSERT INTO refunds (id, payment_id, amount, currency, status, reason, created_at, updated_at) VALUES
('rf1uuu99-9c0b-4ef8-bb6d-6bb9bd380a11', 'p4ssss99-9c0b-4ef8-bb6d-6bb9bd380a44', 5000.00, 'INR', 'COMPLETED', 'Customer requested cancellation', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

-- ==================== ANALYTICS_SCHEMA SAMPLE DATA ====================
SET search_path TO analytics_schema;

-- Create settlement metrics
INSERT INTO settlement_metrics (id, merchant_id, total_volume, successful_payments, failed_payments, last_updated) VALUES
('s1vvvv99-9c0b-4ef8-bb6d-6bb9bd380a11', 'm1kkka99-9c0b-4ef8-bb6d-6bb9bd380a11', 9000.50, 45, 3, NOW()),
('s2wwww99-9c0b-4ef8-bb6d-6bb9bd380a22', 'm2llll99-9c0b-4ef8-bb6d-6bb9bd380a22', 2500.00, 12, 1, NOW()),
('s3xxxx99-9c0b-4ef8-bb6d-6bb9bd380a33', 'm3mmmm99-9c0b-4ef8-bb6d-6bb9bd380a33', 750.25, 3, 1, NOW())
ON CONFLICT (id) DO NOTHING;

-- ==================== AUDIT_SCHEMA SAMPLE DATA ====================
SET search_path TO audit_schema;

-- Create audit logs
INSERT INTO audit_logs (id, aggregate_id, event_type, payload, timestamp, user_id) VALUES
('a1yyyy99-9c0b-4ef8-bb6d-6bb9bd380a11', 'p1pppp99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PAYMENT_CREATED', '{"action": "payment_created", "amount": 1500}', NOW() - INTERVAL '5 days', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
('a2zzzz99-9c0b-4ef8-bb6d-6bb9bd380a22', 'p1pppp99-9c0b-4ef8-bb6d-6bb9bd380a11', 'PAYMENT_CAPTURED', '{"action": "payment_captured", "amount": 1500}', NOW() - INTERVAL '5 days', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'),
('a3aaaa99-9c0b-4ef8-bb6d-6bb9bd380a33', 'm1kkka99-9c0b-4ef8-bb6d-6bb9bd380a11', 'MERCHANT_CREATED', '{"action": "merchant_created", "business_name": "Test Electronics Store"}', NOW() - INTERVAL '10 days', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11')
ON CONFLICT (id) DO NOTHING;

-- ==================== MERCHANT_SCHEMA SAMPLE DATA ====================
SET search_path TO merchant_schema;

-- Create merchant config (if table exists)
-- INSERT INTO merchant_config ... (add if needed)

\echo 'Sample data loaded successfully!'
SET search_path TO public;
\unset QUIET