-- Analytics Events table
CREATE TABLE analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    merchant_id VARCHAR(36),
    order_id VARCHAR(36),
    payment_id VARCHAR(36),
    user_id VARCHAR(36),
    amount DECIMAL(19, 4),
    currency VARCHAR(3),
    status VARCHAR(50),
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_analytics_events_type ON analytics_events(event_type);
CREATE INDEX idx_analytics_events_category ON analytics_events(event_category);
CREATE INDEX idx_analytics_events_merchant ON analytics_events(merchant_id);
CREATE INDEX idx_analytics_events_order ON analytics_events(order_id);
CREATE INDEX idx_analytics_events_created ON analytics_events(created_at);

-- Aggregated metrics table
CREATE TABLE metrics (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    dimension_merchant_id VARCHAR(36),
    dimension_currency VARCHAR(3),
    dimension_status VARCHAR(50),
    dimension_period VARCHAR(20),
    value DOUBLE PRECISION NOT NULL,
    count BIGINT DEFAULT 1,
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    sum_value DOUBLE PRECISION,
    period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(metric_name, dimension_merchant_id, dimension_currency, dimension_status, dimension_period, period_start)
);

CREATE INDEX idx_metrics_name ON metrics(metric_name);
CREATE INDEX idx_metrics_merchant ON metrics(dimension_merchant_id);
CREATE INDEX idx_metrics_period ON metrics(period_start, period_end);

-- Reports table
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(100) NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    report_params JSONB,
    report_data JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    merchant_id VARCHAR(36),
    created_by VARCHAR(36),
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_type ON reports(report_type);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_merchant ON reports(merchant_id);

-- Real-time counters for live metrics
CREATE TABLE real_time_counters (
    id BIGSERIAL PRIMARY KEY,
    counter_name VARCHAR(100) NOT NULL UNIQUE,
    counter_value BIGINT DEFAULT 0,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- KPIs table
CREATE TABLE kpis (
    id BIGSERIAL PRIMARY KEY,
    kpi_name VARCHAR(100) NOT NULL UNIQUE,
    kpi_value DOUBLE PRECISION NOT NULL,
    kpi_unit VARCHAR(20),
    previous_value DOUBLE PRECISION,
    change_percentage DOUBLE PRECISION,
    dimension_merchant_id VARCHAR(36),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kpis_name ON kpis(kpi_name);
CREATE INDEX idx_kpis_merchant ON kpis(dimension_merchant_id);
