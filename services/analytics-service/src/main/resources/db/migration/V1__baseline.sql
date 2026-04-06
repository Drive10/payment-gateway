-- Analytics Service Baseline
CREATE TABLE IF NOT EXISTS risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID,
    risk_score DECIMAL(5,2),
    risk_level VARCHAR(20),
    factors TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) DEFAULT 'PENDING',
    settled_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID,
    reason TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    amount DECIMAL(19,4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
