-- Merchants table
CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    website VARCHAR(500),
    business_type VARCHAR(50),
    business_category VARCHAR(100),
    tax_id VARCHAR(50),
    kyc_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP WITH TIME ZONE,
    kyc_verified_by VARCHAR(100),
    verification_notes TEXT,
    settlement_schedule VARCHAR(30) DEFAULT 'DAILY',
    settlement_enabled BOOLEAN DEFAULT true,
    pricing_tier VARCHAR(30) DEFAULT 'STANDARD',
    transaction_limit DECIMAL(19, 4) DEFAULT 100000.0000,
    daily_limit DECIMAL(19, 4) DEFAULT 1000000.0000,
    monthly_limit DECIMAL(19, 4) DEFAULT 5000000.0000,
    current_month_volume DECIMAL(19, 4) DEFAULT 0.0000,
    current_month_transactions INTEGER DEFAULT 0,
    webhook_url VARCHAR(500),
    webhook_secret VARCHAR(255),
    api_key VARCHAR(255) UNIQUE,
    api_secret_hash VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchants_email ON merchants(email);
CREATE INDEX idx_merchants_status ON merchants(status);
CREATE INDEX idx_merchants_kyc_status ON merchants(kyc_status);

-- Merchant Addresses table
CREATE TABLE merchant_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    address_type VARCHAR(30) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(2) NOT NULL DEFAULT 'IN',
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_addresses_merchant ON merchant_addresses(merchant_id);

-- KYC Documents table
CREATE TABLE kyc_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100),
    file_url VARCHAR(500),
    file_key VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(100),
    rejection_reason TEXT,
    expiry_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kyc_documents_merchant ON kyc_documents(merchant_id);
CREATE INDEX idx_kyc_documents_type ON kyc_documents(document_type);
CREATE INDEX idx_kyc_documents_status ON kyc_documents(status);

-- Bank Accounts table
CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    account_holder_name VARCHAR(255) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    ifsc_code VARCHAR(20),
    routing_number VARCHAR(50),
    swift_code VARCHAR(20),
    is_default BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bank_accounts_merchant ON bank_accounts(merchant_id);

-- Merchant Users (sub-users)
CREATE TABLE merchant_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    password_hash VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(merchant_id, email)
);

CREATE INDEX idx_merchant_users_merchant ON merchant_users(merchant_id);
CREATE INDEX idx_merchant_users_email ON merchant_users(email);
