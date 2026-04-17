-- Order Service Baseline
-- V1__baseline.sql

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    order_reference VARCHAR(40) NOT NULL UNIQUE,
    external_reference VARCHAR(100),
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    customer_email VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(255),
    merchant_id VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_reference ON orders(order_reference);

-- Merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name VARCHAR(120) NOT NULL,
    legal_name VARCHAR(200),
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(40),
    website VARCHAR(255),
    business_type VARCHAR(50),
    business_category VARCHAR(80),
    tax_id VARCHAR(40),
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP,
    kyc_verified_by VARCHAR(120),
    verification_notes TEXT,
    settlement_schedule VARCHAR(30) DEFAULT 'DAILY',
    settlement_enabled BOOLEAN DEFAULT TRUE,
    pricing_tier VARCHAR(30) DEFAULT 'STANDARD',
    transaction_limit DECIMAL(19,4) DEFAULT 100000,
    daily_limit DECIMAL(19,4) DEFAULT 1000000,
    monthly_limit DECIMAL(19,4) DEFAULT 5000000,
    current_month_volume DECIMAL(19,4) DEFAULT 0,
    current_month_transactions INTEGER DEFAULT 0,
    webhook_url VARCHAR(255),
    webhook_secret VARCHAR(120),
    api_key VARCHAR(255) UNIQUE,
    api_secret_hash VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_merchants_email ON merchants(email);
CREATE INDEX idx_merchants_kyc_status ON merchants(kyc_status);
CREATE INDEX idx_merchants_status ON merchants(status);

-- API Keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    key_prefix VARCHAR(16) NOT NULL DEFAULT 'pk_live',
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(512),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    permissions TEXT,
    rate_limit_per_minute INTEGER DEFAULT 100,
    rate_limit_per_day INTEGER DEFAULT 10000,
    ip_whitelist TEXT,
    referer_whitelist TEXT,
    last_used_at TIMESTAMP,
    last_used_ip VARCHAR(45),
    expires_at TIMESTAMP,
    rotated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_merchant_id ON api_keys(merchant_id);
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);

-- KycDocument table
CREATE TABLE IF NOT EXISTS kyc_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_url VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP,
    verified_by VARCHAR(120),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kyc_documents_merchant_id ON kyc_documents(merchant_id);

-- BankAccount table
CREATE TABLE IF NOT EXISTS bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder_name VARCHAR(200) NOT NULL,
    ifsc_code VARCHAR(20),
    branch_name VARCHAR(100),
    account_type VARCHAR(20) DEFAULT 'CURRENT',
    is_verified BOOLEAN DEFAULT FALSE,
    is_primary BOOLEAN DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bank_accounts_merchant_id ON bank_accounts(merchant_id);