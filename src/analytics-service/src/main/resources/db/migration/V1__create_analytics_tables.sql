-- Analytics Service Schema
-- Version: 1
-- Description: Create initial tables for analytics service

CREATE SCHEMA IF NOT EXISTS analytics_schema;
SET search_path TO analytics_schema;

-- Settlement metrics table
CREATE TABLE IF NOT EXISTS settlement_metrics (
    id UUID PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    total_volume DECIMAL(19,4) NOT NULL,
    successful_payments BIGINT NOT NULL,
    failed_payments BIGINT NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_settlement_metrics_merchant_id ON settlement_metrics(merchant_id);
CREATE INDEX IF NOT EXISTS idx_settlement_metrics_last_updated ON settlement_metrics(last_updated);

SET search_path TO public;