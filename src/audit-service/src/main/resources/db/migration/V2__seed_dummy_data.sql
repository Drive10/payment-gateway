-- Seed dummy data for audit service
-- Version: 2
-- Description: Seed audit logs

SET search_path TO audit_schema;

INSERT INTO audit_logs (id, aggregate_id, event_type, payload, timestamp, user_id) VALUES
('ffffffff-ffff-ffff-ffff-ffffffffffff', '77777777-7777-7777-7777-777777777777', 'PAYMENT_CREATED', '{"action": "payment_created", "amount": 1500, "currency": "INR"}', NOW() - INTERVAL '5 days', '33333333-3333-3333-3333-333333333333'),
('11111111-1111-1111-1111-111111111112', '77777777-7777-7777-7777-777777777777', 'PAYMENT_CAPTURED', '{"action": "payment_captured", "amount": 1500, "currency": "INR"}', NOW() - INTERVAL '5 days', '33333333-3333-3333-3333-333333333333'),
('22222222-2222-2222-2222-222222222223', '33333333-3333-3333-3333-333333333333', 'MERCHANT_CREATED', '{"action": "merchant_created", "business_name": "Demo Electronics Store"}', NOW() - INTERVAL '10 days', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

SET search_path TO public;