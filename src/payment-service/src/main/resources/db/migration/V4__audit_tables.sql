-- Audit log tables (replacing MongoDB audit-service)
-- V4__audit_tables.sql

-- Add missing columns to existing audit_logs from V1
DO $$
BEGIN
    -- Add missing columns if they don't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'entity_type') THEN
        ALTER TABLE audit_logs ADD COLUMN entity_type VARCHAR(100);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'entity_id') THEN
        ALTER TABLE audit_logs ADD COLUMN entity_id VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'user_id') THEN
        ALTER TABLE audit_logs ADD COLUMN user_id UUID;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'merchant_id') THEN
        ALTER TABLE audit_logs ADD COLUMN merchant_id UUID;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'details') THEN
        ALTER TABLE audit_logs ADD COLUMN details JSONB;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'ip_address') THEN
        ALTER TABLE audit_logs ADD COLUMN ip_address VARCHAR(45);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'audit_logs' AND column_name = 'user_agent') THEN
        ALTER TABLE audit_logs ADD COLUMN user_agent TEXT;
    END IF;
END $$;

-- Create missing indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event ON audit_logs(event_type, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_merchant ON audit_logs(merchant_id);

-- Payment-specific audit trail
CREATE TABLE IF NOT EXISTS payment_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    amount BIGINT,
    currency VARCHAR(3),
    provider VARCHAR(30),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by UUID
);

CREATE INDEX idx_payment_audit_payment ON payment_audit(payment_id, created_at);
CREATE INDEX idx_payment_audit_event ON payment_audit(event_type, created_at);

-- API key usage audit
CREATE TABLE IF NOT EXISTS api_key_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_key_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_method VARCHAR(10),
    status_code INT,
    response_time_ms INT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_api_key_audit_key ON api_key_audit(api_key_id, created_at);
CREATE INDEX idx_api_key_audit_merchant ON api_key_audit(merchant_id, created_at);