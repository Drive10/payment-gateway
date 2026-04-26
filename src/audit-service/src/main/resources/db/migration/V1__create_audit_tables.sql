-- Audit Service Schema
-- Version: 1
-- Description: Create initial tables for audit service

CREATE SCHEMA IF NOT EXISTS audit_schema;
SET search_path TO audit_schema;

-- Audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id VARCHAR(255)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_audit_logs_aggregate_id ON audit_logs(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);

SET search_path TO public;