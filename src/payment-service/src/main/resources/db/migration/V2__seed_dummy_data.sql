-- Seed dummy data for payment service
-- Version: 2
-- Description: Seed demo payments to test checkout flow

SET search_path TO payment_schema;

-- Insert sample payments (using merchant ID from auth service)
-- Payment 1: CAPTURED (successful payment)
INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, created_at, updated_at, expires_at, simulated, method) VALUES
('77777777-7777-7777-7777-777777777777', 'ORD-DEMO-001', 1500.00, 'INR', 'CAPTURED', '33333333-3333-3333-3333-333333333333', 'corr_demo_001', 'CARD', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW() + INTERVAL '1 day', true, 'CARD')
ON CONFLICT (id) DO NOTHING;

-- Payment 2: AUTHORIZED (ready for capture)
INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, created_at, updated_at, expires_at, simulated, method) VALUES
('88888888-8888-8888-8888-888888888888', 'ORD-DEMO-002', 2500.50, 'INR', 'AUTHORIZED', '33333333-3333-3333-3333-333333333333', 'corr_demo_002', 'UPI', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() + INTERVAL '1 day', true, 'UPI')
ON CONFLICT (id) DO NOTHING;

-- Payment 3: CREATED (new payment ready for checkout)
INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, created_at, updated_at, expires_at, simulated, method) VALUES
('99999999-9999-9999-9999-999999999999', 'ORD-DEMO-003', 999.99, 'USD', 'CREATED', '33333333-3333-3333-3333-333333333333', 'corr_demo_003', 'WALLET', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() + INTERVAL '1 day', false, 'WALLET')
ON CONFLICT (id) DO NOTHING;

-- Payment 4: REFUNDED (refunded payment)
INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, created_at, updated_at, expires_at, simulated, method) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ORD-DEMO-004', 5000.00, 'INR', 'REFUNDED', '33333333-3333-3333-3333-333333333333', 'corr_demo_004', 'CARD', NOW() - INTERVAL '7 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', true, 'CARD')
ON CONFLICT (id) DO NOTHING;

-- Payment 5: FAILED (failed payment)
INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, created_at, updated_at, expires_at, simulated, method) VALUES
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'ORD-DEMO-005', 750.25, 'EUR', 'FAILED', '44444444-4444-4444-4444-444444444444', 'corr_demo_005', 'CARD', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', true, 'CARD')
ON CONFLICT (id) DO NOTHING;

-- Insert sample refunds
INSERT INTO refunds (id, payment_id, refund_id, amount, refunded_amount, currency, status, reason, created_at, updated_at) VALUES
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'refund_001', 5000.00, 5000.00, 'INR', 'COMPLETED', 'Customer requested cancellation', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days')
ON CONFLICT (id) DO NOTHING;

SET search_path TO public;