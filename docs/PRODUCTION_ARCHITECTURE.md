# Production-Grade Fintech Platform Architecture

## Vision
Transform this payment gateway into a production-grade fintech platform like Stripe/Razorpay with:
- Complete money flow traceability
- Merchant lifecycle management
- Real settlement logic
- Developer-first API experience

---

## 1. System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           MERCHANT DASHBOARD                                │
│  (Payments | Settlements | Refunds | Webhooks | API Keys | Balance)       │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────────┐
│                         API GATEWAY                                       │
│                    (Rate Limiting | Auth | Routing)                       │
└──────────────────────────────┬──────────────────────────────────────────┘
                               │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌────────────────┐
│  AUTH SERVICE │     │MERCHANT SERVICE │     │  PAYMENT SVC   │
│               │     │                 │     │                │
│ - Login       │     │ - Onboarding   │     │ - Payment Link │
│ - API Keys    │     │ - KYC Workflow │     │ - Processing   │
│ - Webhooks    │     │ - Limits       │     │ - Webhook Events│
└───────────────┘     └─────────────────┘     └───────┬────────┘
                                                      │
                                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TRANSACTION EVENT BUS (KAFKA)                        │
│  payment.created | payment.captured | payment.failed | refund.created      │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐     ┌─────────────────┐     ┌────────────────┐
│   LEDGER SVC  │     │  SETTLEMENT SVC │     │   RISK SVC     │
│               │     │                 │     │                │
│ - Double Entry│     │ - T+1 Batching  │     │ - Fraud Check  │
│ - Balance Calc│     │ - Bank Transfer │     │ - Velocity     │
│ - Fee Tracking│     │ - Reconciliation │     │ - Limits       │
└───────────────┘     └─────────────────┘     └────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                          INFRASTRUCTURE                                      │
│  PostgreSQL (Ledgers) | Redis (Cache/Rate Limit) | Kafka (Events)         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Money Flow Design

### Payment Flow with Ledger
```
Customer Payment (₹1000)
        │
        ▼
┌──────────────────────────────────────┐
│ 1. Payment Created                  │
│    - Payment entity created          │
│    - Status: CREATED                 │
└──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│ 2. Risk Check                        │
│    - Fraud validation                │
└──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│ 3. Payment Captured (₹1000)          │
│                                      │
│ LEDGER ENTRIES:                      │
│ ─────────────────────────────────    │
│ Dr. Customer Wallet    ₹1000         │
│ Cr. Merchant Receivable ₹970          │ (₹1000 - ₹30 fees)
│ Cr. Platform Fee        ₹30           │
│   - Platform Fee         ₹20          │
│   - Gateway Fee          ₹10          │
└──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│ 4. Settlement Batch (T+1)            │
│                                      │
│ LEDGER ENTRIES:                      │
│ ─────────────────────────────────    │
│ Dr. Merchant Receivable  ₹970         │
│ Cr. Merchant Balance     ₹970          │
└──────────────────────────────────────┘
        │
        ▼
┌──────────────────────────────────────┐
│ 5. Payout to Merchant Bank            │
│                                      │
│ LEDGER ENTRIES:                      │
│ ─────────────────────────────────    │
│ Dr. Merchant Balance    ₹970         │
│ Cr. Bank Settlement     ₹970         │
└──────────────────────────────────────┘
```

---

## 3. Database Schema Design

### Core Tables

```sql
-- MERCHANTS with lifecycle
CREATE TABLE merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    -- KYC
    kyc_status VARCHAR(50) DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMP,
    kyc_verified_by UUID,
    -- Limits
    daily_limit DECIMAL(19,4) DEFAULT 100000,
    monthly_limit DECIMAL(19,4) DEFAULT 1000000,
    daily_volume DECIMAL(19,4) DEFAULT 0,
    monthly_volume DECIMAL(19,4) DEFAULT 0,
    -- Banking
    bank_account_name VARCHAR(255),
    bank_account_number VARCHAR(50),
    bank_ifsc_code VARCHAR(20),
    bank_name VARCHAR(255),
    -- API Keys
    test_api_key VARCHAR(64) UNIQUE,
    live_api_key VARCHAR(64) UNIQUE,
    -- Meta
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- API KEYS for developers
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    key_hash VARCHAR(64) NOT NULL,
    key_prefix VARCHAR(8) NOT NULL,
    name VARCHAR(100),
    mode VARCHAR(20) NOT NULL, -- 'test' or 'live'
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- PAYMENT LINKS with merchant reference
CREATE TABLE payment_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_id VARCHAR(16) UNIQUE NOT NULL,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(500),
    customer_name VARCHAR(100),
    customer_email VARCHAR(255),
    -- Fee calculation
    fee_amount DECIMAL(19,4),
    net_amount DECIMAL(19,4),
    -- Links
    payment_id UUID REFERENCES payments(id),
    created_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    paid_at TIMESTAMP
);

-- FEE STRUCTURE per merchant
CREATE TABLE fee_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    fee_type VARCHAR(20) NOT NULL, -- 'FLAT', 'PERCENTAGE', 'TIERED'
    percentage_rate DECIMAL(5,4), -- e.g., 0.029 for 2.9%
    fixed_amount DECIMAL(19,4), -- e.g., 3.00 for ₹3
    currency VARCHAR(3) DEFAULT 'USD',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- LEDGER ACCOUNTS (Double Entry)
CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_type VARCHAR(50) NOT NULL, -- 'MERCHANT_WALLET', 'PLATFORM_FEE', 'BANK_SETTLEMENT', 'CUSTOMER_ESCROW'
    merchant_id UUID REFERENCES merchants(id),
    currency VARCHAR(3) DEFAULT 'USD',
    balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    available_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    locked_balance DECIMAL(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- LEDGER ENTRIES (Immutable)
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id VARCHAR(36) NOT NULL UNIQUE, -- UUID for traceability
    transaction_id VARCHAR(36) NOT NULL, -- Payment/Refund/Settlement ID
    transaction_type VARCHAR(50) NOT NULL, -- 'PAYMENT', 'REFUND', 'SETTLEMENT', 'FEE'
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    entry_type VARCHAR(10) NOT NULL, -- 'DEBIT' or 'CREDIT'
    amount DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    reference_type VARCHAR(50), -- 'PAYMENT', 'REFUND', 'PAYOUT'
    reference_id UUID,
    description VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- SETTLEMENTS
CREATE TABLE settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id VARCHAR(36) NOT NULL UNIQUE,
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    batch_date DATE NOT NULL,
    total_amount DECIMAL(19,4) NOT NULL,
    total_fees DECIMAL(19,4) NOT NULL DEFAULT 0,
    net_amount DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
    bank_reference VARCHAR(100),
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- DISPUTES
CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id VARCHAR(36) NOT NULL UNIQUE,
    payment_id UUID NOT NULL REFERENCES payments(id),
    merchant_id UUID NOT NULL REFERENCES merchants(id),
    amount DECIMAL(19,4) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'UNDER_REVIEW', 'WON', 'LOST', 'CLOSED'
    customer_response TEXT,
    merchant_response TEXT,
    resolution VARCHAR(50),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## 4. Service Responsibilities

### Auth Service
- Merchant authentication
- API key validation (middleware)
- JWT token management

### Merchant Service
- Merchant onboarding
- KYC workflow
- Daily/monthly limits
- Fee structure management
- API key generation

### Payment Service
- Payment link creation
- Payment processing
- Fee calculation
- Webhook events

### Ledger Service (NEW)
- Double-entry bookkeeping
- Balance calculation
- Transaction history
- Audit trails

### Settlement Service
- Batch creation (T+1)
- Bank transfer simulation
- Settlement reconciliation

### Dispute Service
- Dispute creation
- Response workflow
- Resolution handling

---

## 5. Implementation Priority

1. **Ledger System** - Foundation of money tracking
2. **Fee Engine** - Calculate fees on every transaction
3. **Merchant KYC** - Business verification
4. **Settlement Batch** - Real money movement
5. **API Keys** - Developer experience
6. **Disputes** - Customer protection