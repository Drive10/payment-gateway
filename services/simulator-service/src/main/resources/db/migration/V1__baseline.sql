-- Simulator Service Baseline
CREATE TABLE IF NOT EXISTS payment_simulations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scenario VARCHAR(100) NOT NULL,
    result VARCHAR(50) NOT NULL,
    delay_ms INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
