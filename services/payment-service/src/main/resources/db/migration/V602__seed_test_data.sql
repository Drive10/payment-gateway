-- V602__seed_test_data.sql
-- Seed data for testing all payment scenarios

-- ============================================
-- USERS & ROLES
-- ============================================

INSERT INTO roles (name) VALUES 
    ('ROLE_USER'),
    ('ROLE_ADMIN'),
    ('ROLE_MERCHANT')
ON CONFLICT (name) DO NOTHING;

-- Test users (password: Test@1234 - bcrypt hashed)
INSERT INTO users (email, full_name, password, active, created_at, updated_at) VALUES
    ('admin@payflow.com', 'Admin User', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
    ('merchant@acme.com', 'ACME Corporation', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
    ('john.doe@example.com', 'John Doe', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
    ('jane.smith@example.com', 'Jane Smith', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
    ('bob.wilson@example.com', 'Bob Wilson', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Assign roles
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@payflow.com' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'merchant@acme.com' AND r.name = 'ROLE_MERCHANT'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'john.doe@example.com' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'jane.smith@example.com' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;
INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'bob.wilson@example.com' AND r.name = 'ROLE_USER'
ON CONFLICT DO NOTHING;

-- ============================================
-- API CLIENTS
-- ============================================

INSERT INTO api_clients (id, client_code, display_name, api_key, webhook_url, scopes, status, created_at, updated_at) VALUES
    ('a1111111-1111-1111-1111-111111111111', 'ACME_CLIENT', 'ACME Corporation API', 'sk_live_acme_1234567890abcdef', 'https://acme.example.com/webhooks/payment', 'PAYMENTS_READ,PAYMENTS_WRITE,REFUNDS_READ', 'ACTIVE', NOW(), NOW()),
    ('a2222222-2222-2222-2222-222222222222', 'TEST_CLIENT', 'Test Application', 'sk_test_client_abcdef123456', 'https://test.example.com/webhooks', 'PAYMENTS_READ,PAYMENTS_WRITE', 'ACTIVE', NOW(), NOW()),
    ('a3333333-3333-3333-3333-333333333333', 'DEMO_CLIENT', 'Demo Application', 'sk_demo_demo123456789', '', 'PAYMENTS_READ', 'ACTIVE', NOW(), NOW())
ON CONFLICT (client_code) DO NOTHING;

-- ============================================
-- LEDGER ACCOUNTS
-- ============================================

INSERT INTO ledger_accounts (id, account_code, account_name, type, created_at, updated_at) VALUES
    ('b1111111-1111-1111-1111-111111111111', 'CASH', 'Cash Account', 'ASSET', NOW(), NOW()),
    ('b2222222-2222-2222-2222-222222222222', 'CUSTOMER_FUNDS', 'Customer Funds', 'LIABILITY', NOW(), NOW()),
    ('b3333333-3333-3333-3333-333333333333', 'MERCHANT_RECEIVABLE', 'Merchant Receivable', 'ASSET', NOW(), NOW()),
    ('b4444444-4444-4444-4444-444444444444', 'REVENUE', 'Revenue Account', 'REVENUE', NOW(), NOW()),
    ('b5555555-5555-5555-5555-555555555555', 'PLATFORM_FEE', 'Platform Fee', 'REVENUE', NOW(), NOW())
ON CONFLICT (account_code) DO NOTHING;

-- ============================================
-- ORDERS (various states)
-- ============================================

INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c1111111-1111-1111-1111-111111111111', 'ORD_20240101_001', 'ext-order-001', 5000.00, 'INR', 'CREATED', 'Premium Subscription - Monthly', id, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days' FROM users WHERE email = 'john.doe@example.com'
ON CONFLICT (order_reference) DO NOTHING;
INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c2222222-2222-2222-2222-222222222222', 'ORD_20240101_002', 'ext-order-002', 15000.00, 'INR', 'CREATED', 'Annual Plan Upgrade', id, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days' FROM users WHERE email = 'jane.smith@example.com'
ON CONFLICT (order_reference) DO NOTHING;
INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c3333333-3333-3333-3333-333333333333', 'ORD_20240102_001', 'ext-order-003', 2500.00, 'INR', 'CREATED', 'One-time Purchase', id, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days' FROM users WHERE email = 'bob.wilson@example.com'
ON CONFLICT (order_reference) DO NOTHING;
INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c4444444-4444-4444-4444-444444444444', 'ORD_20240103_001', 'ext-order-004', 10000.00, 'INR', 'COMPLETED', 'Product Purchase', id, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days' FROM users WHERE email = 'john.doe@example.com'
ON CONFLICT (order_reference) DO NOTHING;
INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c5555555-5555-5555-5555-555555555555', 'ORD_20240104_001', 'ext-order-005', 7500.00, 'INR', 'COMPLETED', 'Service Subscription', id, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days' FROM users WHERE email = 'jane.smith@example.com'
ON CONFLICT (order_reference) DO NOTHING;
INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c6666666-6666-6666-6666-666666666666', 'ORD_20240105_001', 'ext-order-006', 20000.00, 'INR', 'COMPLETED', 'Enterprise License', id, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day' FROM users WHERE email = 'bob.wilson@example.com'
ON CONFLICT (order_reference) DO NOTHING;
INSERT INTO orders (id, order_reference, external_reference, amount, currency, status, description, user_id, created_at, updated_at) 
SELECT 'c7777777-7777-7777-7777-777777777777', 'ORD_20240106_001', 'ext-order-007', 5000.00, 'INR', 'CANCELLED', 'Cancelled Order', id, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day' FROM users WHERE email = 'john.doe@example.com'
ON CONFLICT (order_reference) DO NOTHING;

-- ============================================
-- PAYMENTS (various providers, methods, states)
-- ============================================

-- Card payments
INSERT INTO payments (id, order_id, provider_order_id, provider_payment_id, idempotency_key, amount, refunded_amount, currency, provider, method, status, transaction_mode, simulated, provider_signature, checkout_url, notes, version, created_at, updated_at) VALUES
    -- Captured card payments (completed orders)
    ('d1111111-1111-1111-1111-111111111111', 'c4444444-4444-4444-4444-444444444444', 'prov_order_001', 'prov_pay_001', 'idem_001', 10000.00, 0, 'INR', 'SIMULATOR', 'CARD', 'CAPTURED', 'PRODUCTION', false, 'sig_001', 'https://checkout.payflow.com/pay/prov_order_001', 'Test payment', 2, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),
    ('d2222222-2222-2222-2222-222222222222', 'c5555555-5555-5555-5555-555555555555', 'prov_order_002', 'prov_pay_002', 'idem_002', 7500.00, 2500.00, 'INR', 'RAZORPAY', 'CARD', 'PARTIALLY_REFUNDED', 'PRODUCTION', false, 'sig_002', 'https://checkout.razorpay.com/pay/prov_order_002', 'With partial refund', 3, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'),
    
    -- UPI payments
    ('d3333333-3333-3333-3333-333333333333', 'c6666666-6666-6666-6666-666666666666', 'prov_order_003', 'prov_pay_003', 'idem_003', 20000.00, 0, 'INR', 'PHONEPE', 'UPI', 'CAPTURED', 'PRODUCTION', false, 'sig_003', 'https://checkout.phonepe.com/pay/prov_order_003', 'UPI payment', 1, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'),
    
    -- Pending payments (created but not captured)
    ('d4444444-4444-4444-4444-444444444444', 'c1111111-1111-1111-1111-111111111111', 'prov_order_004', NULL, 'idem_004', 5000.00, 0, 'INR', 'SIMULATOR', 'CARD', 'CREATED', 'TEST', true, NULL, 'https://simulator.test/checkout/prov_order_004', 'Pending test payment', 1, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('d5555555-5555-5555-5555-555555555555', 'c2222222-2222-2222-2222-222222222222', 'prov_order_005', NULL, 'idem_005', 15000.00, 0, 'INR', 'RAZORPAY', 'UPI', 'CREATED', 'PRODUCTION', false, NULL, 'https://checkout.razorpay.com/pay/prov_order_005', 'Pending UPI', 1, NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('d6666666-6666-6666-6666-666666666666', 'c3333333-3333-3333-3333-333333333333', 'prov_order_006', NULL, 'idem_006', 2500.00, 0, 'INR', 'PHONEPE', 'WALLET', 'CREATED', 'TEST', true, NULL, 'https://simulator.test/checkout/prov_order_006', 'Pending wallet', 1, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    
    -- Failed payment
    ('d7777777-7777-7777-7777-777777777777', 'c7777777-7777-7777-7777-777777777777', 'prov_order_007', NULL, 'idem_007', 5000.00, 0, 'INR', 'SIMULATOR', 'CARD', 'FAILED', 'PRODUCTION', false, NULL, 'https://simulator.test/checkout/prov_order_007', 'Failed payment', 1, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    
    -- Fully refunded payment
    ('d8888888-8888-8888-8888-888888888888', 'c4444444-4444-4444-4444-444444444444', 'prov_order_008', 'prov_pay_008', 'idem_008', 5000.00, 5000.00, 'INR', 'SIMULATOR', 'CARD', 'REFUNDED', 'PRODUCTION', false, 'sig_008', 'https://checkout.payflow.com/pay/prov_order_008', 'Fully refunded', 4, NOW() - INTERVAL '10 days', NOW() - INTERVAL '5 days')
ON CONFLICT (provider_order_id) DO NOTHING;

-- ============================================
-- PAYMENT TRANSACTIONS (event log)
-- ============================================

INSERT INTO transactions (id, payment_id, type, status, amount, provider_reference, remarks, created_at, updated_at) VALUES
    -- Order d1111111 - captured card
    ('e1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111', 'PAYMENT_INITIATED', 'PENDING', 10000.00, 'prov_order_001', 'Payment initiated', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('e1111112-1111-1111-1111-111111111112', 'd1111111-1111-1111-1111-111111111111', 'PAYMENT_CAPTURED', 'SUCCESS', 10000.00, 'prov_pay_001', 'Payment captured successfully', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    
    -- Order d2222222 - partially refunded
    ('e2222221-2222-2222-2222-222222222221', 'd2222222-2222-2222-2222-222222222222', 'PAYMENT_INITIATED', 'PENDING', 7500.00, 'prov_order_002', 'Payment initiated', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('e2222222-2222-2222-2222-222222222222', 'd2222222-2222-2222-2222-222222222222', 'PAYMENT_CAPTURED', 'SUCCESS', 7500.00, 'prov_pay_002', 'Payment captured', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('e2222223-2222-2222-2222-222222222223', 'd2222222-2222-2222-2222-222222222222', 'REFUND_INITIATED', 'PENDING', 2500.00, 'ref_001', 'Partial refund requested', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('e2222224-2222-2222-2222-222222222224', 'd2222222-2222-2222-2222-222222222222', 'REFUND_COMPLETED', 'SUCCESS', 2500.00, 'ref_001', 'Partial refund completed', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    
    -- Order d3333333 - UPI
    ('e3333331-3333-3333-3333-333333333331', 'd3333333-3333-3333-3333-333333333333', 'PAYMENT_INITIATED', 'PENDING', 20000.00, 'prov_order_003', 'UPI payment initiated', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('e3333332-3333-3333-3333-333333333332', 'd3333333-3333-3333-3333-333333333333', 'PAYMENT_CAPTURED', 'SUCCESS', 20000.00, 'prov_pay_003', 'UPI payment captured', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    
    -- Order d4444444 - pending test
    ('e4444441-4444-4444-4444-444444444441', 'd4444444-4444-4444-4444-444444444444', 'PAYMENT_INITIATED', 'PENDING', 5000.00, 'prov_order_004', 'Test payment created', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    
    -- Order d7777777 - failed
    ('e7777771-7777-7777-7777-777777777771', 'd7777777-7777-7777-7777-777777777777', 'PAYMENT_INITIATED', 'PENDING', 5000.00, 'prov_order_007', 'Payment initiated', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('e7777772-7777-7777-7777-777777777772', 'd7777777-7777-7777-7777-777777777777', 'PAYMENT_FAILED', 'FAILED', 5000.00, 'prov_order_007', 'Payment declined', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    
    -- Order d8888888 - fully refunded
    ('e8888881-8888-8888-8888-888888888881', 'd8888888-8888-8888-8888-888888888888', 'PAYMENT_INITIATED', 'PENDING', 5000.00, 'prov_order_008', 'Payment initiated', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('e8888882-8888-8888-8888-888888888882', 'd8888888-8888-8888-8888-888888888888', 'PAYMENT_CAPTURED', 'SUCCESS', 5000.00, 'prov_pay_008', 'Captured', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('e8888883-8888-8888-8888-888888888883', 'd8888888-8888-8888-8888-888888888888', 'REFUND_INITIATED', 'PENDING', 5000.00, 'ref_002', 'Refund requested', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days'),
    ('e8888884-8888-8888-8888-888888888884', 'd8888888-8888-8888-8888-888888888888', 'REFUND_COMPLETED', 'SUCCESS', 5000.00, 'ref_002', 'Full refund completed', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days')
ON CONFLICT DO NOTHING;

-- ============================================
-- REFUNDS
-- ============================================

INSERT INTO payment_refunds (id, payment_id, amount, status, provider_refund_id, idempotency_key, refund_reference, reason, created_at, updated_at) VALUES
    ('f1111111-1111-1111-1111-111111111111', 'd2222222-2222-2222-2222-222222222222', 2500.00, 'COMPLETED', 'ref_001', 'refund_idem_001', 'refund_001', 'Customer request', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('f2222222-2222-2222-2222-222222222222', 'd8888888-8888-8888-8888-888888888888', 5000.00, 'COMPLETED', 'ref_002', 'refund_idem_002', 'refund_002', 'Product return', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days')
ON CONFLICT DO NOTHING;

-- ============================================
-- SIMULATION TRANSACTIONS
-- ============================================

INSERT INTO simulation_transactions (id, order_reference, payment_reference, provider, provider_order_id, provider_payment_id, provider_signature, simulation_mode, status, amount, currency, checkout_url, notes, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'sim_order_001', 'sim_pay_001', 'SIMULATOR', 'prov_order_001', 'prov_pay_001', 'sig_001', 'PRODUCTION', 'COMPLETED', 10000.00, 'INR', 'https://simulator.test/checkout/prov_order_001', 'Simulated payment', NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222222', 'sim_order_002', 'sim_pay_002', 'SIMULATOR', 'prov_order_004', NULL, NULL, 'TEST', 'PENDING', 5000.00, 'INR', 'https://simulator.test/checkout/prov_order_004', 'Pending test', NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('33333333-3333-3333-3333-333333333333', 'sim_order_003', 'sim_pay_003', 'SIMULATOR', 'prov_order_007', NULL, NULL, 'PRODUCTION', 'FAILED', 5000.00, 'INR', 'https://simulator.test/checkout/prov_order_007', 'Failed simulation', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day')
ON CONFLICT (provider_order_id) DO NOTHING;

-- ============================================
-- LEDGER JOURNAL ENTRIES
-- ============================================

INSERT INTO journal_entries (id, reference, debit_account_code, credit_account_code, amount, narration, created_at, updated_at) VALUES
    ('a1111111-1111-1111-1111-111111111111', 'PAY-d1111111', 'CASH', 'CUSTOMER_FUNDS', 10000.00, 'Payment received for order ORD_20240103_001', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('a1111112-1111-1111-1111-111111111112', 'PAY-d2222222', 'CASH', 'CUSTOMER_FUNDS', 7500.00, 'Payment received for order ORD_20240104_001', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days')
ON CONFLICT (reference) DO NOTHING;

-- ============================================
-- NOTIFICATIONS TEMPLATES
-- ============================================

INSERT INTO templates (id, template_code, channel, subject, body, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111112', 'PAYMENT_SUCCESS', 'EMAIL', 'Payment Received - {{orderReference}}', 'Dear {{customerName}}, your payment of {{amount}} {{currency}} for order {{orderReference}} has been received.', NOW(), NOW()),
    ('11111111-1111-1111-1111-111111111113', 'PAYMENT_FAILED', 'EMAIL', 'Payment Failed - {{orderReference}}', 'Dear {{customerName}}, your payment for order {{orderReference}} has failed. Please try again.', NOW(), NOW()),
    ('11111111-1111-1111-1111-111111111114', 'REFUND_INITIATED', 'EMAIL', 'Refund Initiated - {{orderReference}}', 'Dear {{customerName}}, your refund of {{amount}} {{currency}} has been initiated.', NOW(), NOW())
ON CONFLICT (template_code) DO NOTHING;

-- Reset sequence to avoid conflicts (optional, only if needed)
-- SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM users));
