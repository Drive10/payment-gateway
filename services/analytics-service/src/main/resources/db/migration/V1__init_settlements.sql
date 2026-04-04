CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_reference VARCHAR(50) UNIQUE NOT NULL,
    merchant_id UUID NOT NULL,
    merchant_name VARCHAR(255),
    total_transactions INTEGER NOT NULL DEFAULT 0,
    total_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_fees DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_refunds DECIMAL(19, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    bank_name VARCHAR(255),
    payout_reference VARCHAR(100),
    payout_date TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlements_merchant ON settlements(merchant_id);
CREATE INDEX idx_settlements_status ON settlements(status);
CREATE INDEX idx_settlements_period ON settlements(period_start, period_end);
CREATE INDEX idx_settlements_created ON settlements(created_at);

CREATE TABLE settlement_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL REFERENCES settlements(id),
    transaction_id UUID NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    fee DECIMAL(19, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    transaction_type VARCHAR(20) NOT NULL DEFAULT 'PAYMENT',
    status VARCHAR(20) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settlement_transactions_settlement ON settlement_transactions(settlement_id);
CREATE INDEX idx_settlement_transactions_tx ON settlement_transactions(transaction_id);

CREATE TABLE merchant_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID UNIQUE NOT NULL,
    merchant_name VARCHAR(255) NOT NULL,
    bank_account_number VARCHAR(50),
    bank_ifsc VARCHAR(20),
    bank_name VARCHAR(255),
    current_balance DECIMAL(19, 2) NOT NULL DEFAULT 0,
    pending_balance DECIMAL(19, 2) NOT NULL DEFAULT 0,
    total_settled DECIMAL(19, 2) NOT NULL DEFAULT 0,
    settlement_frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    auto_settle BOOLEAN NOT NULL DEFAULT true,
    minimum_settlement DECIMAL(19, 2) NOT NULL DEFAULT 1000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_settlements_balance ON merchant_settlements(current_balance);
