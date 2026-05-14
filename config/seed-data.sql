-- Seed dev data for local development
-- Version: 3
-- Description: Seed comprehensive demo data for testing

-- Use the same schema JPA entities use
SET search_path TO public;

-- ── Merchant UUID (consistent across seeds) ──
-- UUID derived from "merchant@test.com"
-- SELECT uuid('33333333-3333-3333-3333-333333333333');

-- ── Seed payments only if empty ──
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM payments) = 0 THEN
        -- CAPTURED - successful payment
        INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, method, simulated, transaction_mode, platform_fee, gateway_fee, refund_amount, created_at, updated_at, captured_at, provider_reference)
        VALUES ('a0000000-0000-0000-0000-000000000001', 'ORD-DEMO-001', 5000.0000, 'INR', 'CAPTURED', '33333333-3333-3333-3333-333333333333', 'corr_seed_001', 'CARD', 'CARD', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'PROV_a1b2c3d4');

        -- CAPTURED - USD payment
        INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, method, simulated, transaction_mode, platform_fee, gateway_fee, refund_amount, created_at, updated_at, captured_at, provider_reference)
        VALUES ('a0000000-0000-0000-0000-000000000002', 'ORD-DEMO-002', 1200.0000, 'USD', 'CAPTURED', '33333333-3333-3333-3333-333333333333', 'corr_seed_002', 'CARD', 'CARD', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', 'PROV_e5f6g7h8');

        -- AUTHORIZED - ready to capture
        INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, method, simulated, transaction_mode, platform_fee, gateway_fee, refund_amount, created_at, updated_at)
        VALUES ('a0000000-0000-0000-0000-000000000003', 'ORD-DEMO-003', 2500.0000, 'INR', 'AUTHORIZED', '33333333-3333-3333-3333-333333333333', 'corr_seed_003', 'UPI', 'UPI', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

        -- FAILED - declined payment
        INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, method, simulated, transaction_mode, platform_fee, gateway_fee, refund_amount, failure_reason, created_at, updated_at)
        VALUES ('a0000000-0000-0000-0000-000000000004', 'ORD-DEMO-004', 800.0000, 'EUR', 'FAILED', '33333333-3333-3333-3333-333333333333', 'corr_seed_004', 'CARD', 'CARD', true, 'TEST', 0, 0, 0, 'Card declined by issuer', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days');

        -- CREATED - new payment
        INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, method, simulated, transaction_mode, platform_fee, gateway_fee, refund_amount, created_at, updated_at)
        VALUES ('a0000000-0000-0000-0000-000000000005', 'ORD-DEMO-005', 9999.0000, 'INR', 'CREATED', '33333333-3333-3333-3333-333333333333', 'corr_seed_005', 'CARD', 'CARD', false, 'TEST', 0, 0, 0, NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours');

        -- Additional recent transactions for dashboard testing
        INSERT INTO payments (id, order_id, amount, currency, status, merchant_id, correlation_id, payment_method, method, simulated, transaction_mode, platform_fee, gateway_fee, refund_amount, created_at, updated_at, captured_at)
        VALUES
            ('b0000000-0000-0000-0000-000000000001', 'ORD-DASH-001', 1500.0000, 'INR', 'CAPTURED', '33333333-3333-3333-3333-333333333333', 'corr_dash_001', 'CARD', 'CARD', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours'),
            ('b0000000-0000-0000-0000-000000000002', 'ORD-DASH-002', 3200.0000, 'INR', 'CAPTURED', '33333333-3333-3333-3333-333333333333', 'corr_dash_002', 'UPI', 'UPI', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
            ('b0000000-0000-0000-0000-000000000003', 'ORD-DASH-003', 750.0000, 'INR', 'FAILED', '33333333-3333-3333-3333-333333333333', 'corr_dash_003', 'CARD', 'CARD', true, 'TEST', 0, 0, 0, 'Insufficient funds', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
            ('b0000000-0000-0000-0000-000000000004', 'ORD-DASH-004', 11000.0000, 'INR', 'CAPTURED', '33333333-3333-3333-3333-333333333333', 'corr_dash_004', 'NETBANKING', 'NETBANKING', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
            ('b0000000-0000-0000-0000-000000000005', 'ORD-DASH-005', 450.0000, 'USD', 'AUTHORIZED', '33333333-3333-3333-3333-333333333333', 'corr_dash_005', 'CARD', 'CARD', true, 'TEST', 0, 0, 0, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days');
    END IF;
END $$;

-- ── Seed plans ──
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM plans) = 0 THEN
        INSERT INTO plans (id, name, description, amount, currency, interval, trial_period_days, status, merchant_id, created_at, updated_at)
        VALUES
            ('p0000000-0000-0000-0000-000000000001', 'Starter Monthly', 'Perfect for small businesses. Includes basic payment processing and standard support.', 999.0000, 'INR', 'monthly', 14, 'active', '33333333-3333-3333-3333-333333333333', NOW(), NOW()),
            ('p0000000-0000-0000-0000-000000000002', 'Pro Monthly', 'For growing businesses. Advanced analytics, priority support, and team access.', 2999.0000, 'INR', 'monthly', 7, 'active', '33333333-3333-3333-3333-333333333333', NOW(), NOW()),
            ('p0000000-0000-0000-0000-000000000003', 'Enterprise Yearly', 'Complete solution for large organizations. Custom integrations, SLA, and dedicated support.', 24000.0000, 'INR', 'yearly', 30, 'active', '33333333-3333-3333-3333-333333333333', NOW(), NOW()),
            ('p0000000-0000-0000-0000-000000000004', 'Basic Weekly', 'Entry-level plan for testing and small scale operations.', 299.0000, 'INR', 'weekly', null, 'active', '33333333-3333-3333-3333-333333333333', NOW(), NOW()),
            ('p0000000-0000-0000-0000-000000000005', 'Starter USD', 'US market starter plan with USD billing.', 19.0000, 'USD', 'monthly', 14, 'active', '33333333-3333-3333-3333-333333333333', NOW(), NOW());
    END IF;
END $$;

-- ── Seed subscriptions ──
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM subscriptions) = 0 THEN
        INSERT INTO subscriptions (id, subscription_id, merchant_id, customer_email, customer_name, plan_id, plan_name, amount, currency, interval, status, current_period_start, current_period_end, billing_cycles, billing_cycles_done, created_at, updated_at)
        VALUES
            ('s0000000-0000-0000-0000-000000000001', 'sub_seed_001', '33333333-3333-3333-3333-333333333333', 'alice@example.com', 'Alice Johnson', 'p0000000-0000-0000-0000-000000000002', 'Pro Monthly', 2999.0000, 'INR', 'monthly', 'ACTIVE', NOW() - INTERVAL '30 days', NOW() + INTERVAL '25 days', null, 1, NOW() - INTERVAL '30 days', NOW()),
            ('s0000000-0000-0000-0000-000000000002', 'sub_seed_002', '33333333-3333-3333-3333-333333333333', 'bob@example.com', 'Bob Smith', 'p0000000-0000-0000-0000-000000000001', 'Starter Monthly', 999.0000, 'INR', 'monthly', 'ACTIVE', NOW() - INTERVAL '15 days', NOW() + INTERVAL '10 days', null, 0, NOW() - INTERVAL '15 days', NOW()),
            ('s0000000-0000-0000-0000-000000000003', 'sub_seed_003', '33333333-3333-3333-3333-333333333333', 'carol@example.com', 'Carol Davis', 'p0000000-0000-0000-0000-000000000003', 'Enterprise Yearly', 24000.0000, 'INR', 'yearly', 'TRIALING', NOW() - INTERVAL '5 days', NOW() + INTERVAL '25 days', 12, 0, NOW() - INTERVAL '5 days', NOW()),
            ('s0000000-0000-0000-0000-000000000004', 'sub_seed_004', '33333333-3333-3333-3333-333333333333', 'dave@example.com', 'Dave Wilson', 'p0000000-0000-0000-0000-000000000002', 'Pro Monthly', 2999.0000, 'INR', 'monthly', 'PAUSED', NOW() - INTERVAL '60 days', NOW() - INTERVAL '5 days', null, 2, NOW() - INTERVAL '60 days', NOW()),
            ('s0000000-0000-0000-0000-000000000005', 'sub_seed_005', '33333333-3333-3333-3333-333333333333', 'eve@example.com', 'Eve Brown', 'p0000000-0000-0000-0000-000000000001', 'Starter Monthly', 999.0000, 'INR', 'monthly', 'CANCELLED', NOW() - INTERVAL '90 days', NOW() - INTERVAL '60 days', 3, 3, NOW() - INTERVAL '90 days', NOW() - INTERVAL '60 days');
    END IF;
END $$;

-- ── Seed invoices ──
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM invoices) = 0 THEN
        INSERT INTO invoices (id, invoice_number, merchant_id, customer_email, customer_name, amount, currency, status, description, due_date, paid_at, created_at, updated_at)
        VALUES
            ('i0000000-0000-0000-0000-000000000001', 'INV-2026-0001', '33333333-3333-3333-3333-333333333333', 'alice@example.com', 'Alice Johnson', 2999.0000, 'INR', 'PAID', 'Pro Monthly - May 2026', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '35 days', NOW()),
            ('i0000000-0000-0000-0000-000000000002', 'INV-2026-0002', '33333333-3333-3333-3333-333333333333', 'bob@example.com', 'Bob Smith', 999.0000, 'INR', 'PENDING', 'Starter Monthly - June 2026', NOW() + INTERVAL '10 days', null, NOW() - INTERVAL '5 days', NOW()),
            ('i0000000-0000-0000-0000-000000000003', 'INV-2026-0003', '33333333-3333-3333-3333-333333333333', 'acme@corp.com', 'Acme Corp', 45000.0000, 'INR', 'OVERDUE', 'Enterprise Yearly - Q2 2026', NOW() - INTERVAL '5 days', null, NOW() - INTERVAL '30 days', NOW()),
            ('i0000000-0000-0000-0000-000000000004', 'INV-2026-0004', '33333333-3333-3333-3333-333333333333', 'sara@email.com', 'Sara L.', 2999.0000, 'INR', 'DRAFT', 'Consulting services - May 2026', NOW() + INTERVAL '15 days', null, NOW(), NOW()),
            ('i0000000-0000-0000-0000-000000000005', 'INV-2026-0005', '33333333-3333-3333-3333-333333333333', 'beta@corp.com', 'Beta Corp', 7500.0000, 'INR', 'PAID', 'Design services - April 2026', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days', NOW() - INTERVAL '50 days', NOW());
    END IF;
END $$;

-- ── Seed saved payment methods ──
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM saved_payment_methods) = 0 THEN
        INSERT INTO saved_payment_methods (id, token, customer_email, type, card_last4, card_brand, expiry_month, expiry_year, cardholder_name, is_default, is_expired, created_at, updated_at)
        VALUES
            ('v0000000-0000-0000-0000-000000000001', 'pm_seed_card_001', 'alice@example.com', 'card', '4242', 'Visa', 12, 2028, 'Alice Johnson', true, false, NOW() - INTERVAL '30 days', NOW()),
            ('v0000000-0000-0000-0000-000000000002', 'pm_seed_card_002', 'alice@example.com', 'card', '1111', 'Mastercard', 8, 2027, 'Alice Johnson', false, false, NOW() - INTERVAL '15 days', NOW()),
            ('v0000000-0000-0000-0000-000000000003', 'pm_seed_upi_001', 'bob@example.com', 'upi', null, null, null, null, null, true, false, NOW() - INTERVAL '20 days', NOW()),
            ('v0000000-0000-0000-0000-000000000004', 'pm_seed_card_003', 'carol@example.com', 'card', '9999', 'RuPay', 3, 2029, 'Carol Davis', true, false, NOW() - INTERVAL '45 days', NOW()),
            ('v0000000-0000-0000-0000-000000000005', 'pm_seed_card_004', 'carol@example.com', 'card', '5555', 'Amex', 6, 2028, 'Carol Davis', false, false, NOW() - INTERVAL '10 days', NOW());
    END IF;
END $$;

-- ── Seed ledger entries for finance dashboard ──
DO $$
BEGIN
    IF (SELECT COUNT(*) FROM ledger_entries) = 0 THEN
        INSERT INTO ledger_entries (id, entry_id, account_id, account_type, entry_type, reference_id, reference_type, amount, currency, merchant_id, payment_id, created_at, posted_at, journal_id)
        VALUES
            ('l0000000-0000-0000-0000-000000000001', 'ledger_seed_001', '33333333-3333-3333-3333-333333333333', 'MERCHANT_RECEIVABLE', 'CREDIT', 'a0000000-0000-0000-0000-000000000001', 'PAYMENT', 5000.0000, 'INR', '33333333-3333-3333-3333-333333333333', 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'j0000000-0000-0000-0000-000000000001'),
            ('l0000000-0000-0000-0000-000000000002', 'ledger_seed_002', '33333333-3333-3333-3333-333333333333', 'MERCHANT_RECEIVABLE', 'CREDIT', 'a0000000-0000-0000-0000-000000000002', 'PAYMENT', 1200.0000, 'USD', '33333333-3333-3333-3333-333333333333', 'a0000000-0000-0000-0000-000000000002', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', 'j0000000-0000-0000-0000-000000000002'),
            ('l0000000-0000-0000-0000-000000000003', 'ledger_seed_003', '33333333-3333-3333-3333-333333333333', 'PLATFORM_FEE_RECEIVABLE', 'DEBIT', 'a0000000-0000-0000-0000-000000000001', 'FEE', 50.0000, 'INR', '33333333-3333-3333-3333-333333333333', 'a0000000-0000-0000-0000-000000000001', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', 'j0000000-0000-0000-0000-000000000001'),
            ('l0000000-0000-0000-0000-000000000004', 'ledger_seed_004', '33333333-3333-3333-3333-333333333333', 'CUSTOMER_ESCROW', 'DEBIT', 'a0000000-0000-0000-0000-000000000003', 'PAYMENT', 2500.0000, 'INR', '33333333-3333-3333-3333-333333333333', 'a0000000-0000-0000-0000-000000000003', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 'j0000000-0000-0000-0000-000000000003');
    END IF;
END $$;

SET search_path TO public;
