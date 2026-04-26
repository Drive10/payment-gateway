-- Seed dummy data for analytics service
-- Version: 2
-- Description: Seed settlement metrics

SET search_path TO analytics_schema;

INSERT INTO settlement_metrics (id, merchant_id, total_volume, successful_payments, failed_payments, last_updated) VALUES
('dddddddd-dddd-dddd-dddd-dddddddddddd', '33333333-3333-3333-3333-333333333333', 9000.50, 45, 3, NOW()),
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '44444444-4444-4444-4444-444444444444', 2500.00, 12, 1, NOW())
ON CONFLICT (id) DO NOTHING;

SET search_path TO public;