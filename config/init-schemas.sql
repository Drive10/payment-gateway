-- Initialize schemas for each service
-- Tables are created by Hibernate ddl-auto=update via Spring docker profile
-- This script runs on first startup of the PostgreSQL container

CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS payment_schema;
CREATE SCHEMA IF NOT EXISTS analytics_schema;
CREATE SCHEMA IF NOT EXISTS audit_schema;

DO $$
BEGIN
    RAISE NOTICE 'Service schemas initialized successfully';
END $$;
