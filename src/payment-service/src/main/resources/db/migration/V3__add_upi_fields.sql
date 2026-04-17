-- Add UPI fields to payments table
-- V3__add_upi_fields.sql

ALTER TABLE payments ADD COLUMN IF NOT EXISTS upi_id VARCHAR(50);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS upi_link VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_payments_upi_id ON payments(upi_id);
CREATE INDEX IF NOT EXISTS idx_payments_status_upi ON payments(status, method) WHERE method = 'UPI';