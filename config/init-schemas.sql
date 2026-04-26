-- Initialize schemas for each service
-- This script runs on first startup of the PostgreSQL container

-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS analytics_schema;
CREATE SCHEMA IF NOT EXISTS audit_schema;
CREATE SCHEMA IF NOT EXISTS merchant_schema;

-- Grant necessary permissions (if needed for future multi-user setup)
-- GRANT USAGE ON SCHEMA auth_schema TO payflow;
-- GRANT USAGE ON SCHEMA payment_schema TO payflow;
-- GRANT USAGE ON SCHEMA analytics_schema TO payflow;
-- GRANT USAGE ON SCHEMA audit_schema TO payflow;
-- GRANT USAGE ON SCHEMA merchant_schema TO payflow;

-- Set search path for convenience (optional)
-- ALTER DATABASE payflow SET search_path TO auth_schema, payment_schema, analytics_schema, audit_schema, merchant_schema, public;

DO $$
BEGIN
    RAISE NOTICE 'Service schemas initialized successfully';
END $$;