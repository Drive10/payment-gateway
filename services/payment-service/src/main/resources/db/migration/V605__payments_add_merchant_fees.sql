-- Add merchant and fee fields to payments table
ALTER TABLE payments ADD COLUMN IF NOT EXISTS merchant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS platform_fee numeric(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_fee numeric(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS pricing_tier varchar(32) NOT NULL DEFAULT 'STANDARD';

CREATE INDEX IF NOT EXISTS idx_payments_merchant_id ON payments(merchant_id);
