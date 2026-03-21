CREATE TABLE risk_assessments
(
    id UUID PRIMARY KEY,
    merchant_reference VARCHAR(80) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    country_code VARCHAR(8) NOT NULL,
    velocity_count INTEGER NOT NULL,
    risk_score INTEGER NOT NULL,
    decision VARCHAR(32) NOT NULL,
    reasons VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
