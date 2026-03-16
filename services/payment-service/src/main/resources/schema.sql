-- ============================================
-- PAYMENTS TABLE
-- ============================================

CREATE TABLE payments
(
    id                  UUID PRIMARY KEY,
    order_id            UUID        NOT NULL,
    provider            VARCHAR(50),
    provider_payment_id VARCHAR(255),
    amount              BIGINT      NOT NULL,
    currency            VARCHAR(10) NOT NULL,
    status              VARCHAR(50),
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TRANSACTIONS
-- ============================================

CREATE TABLE transactions
(
    id         UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    type       VARCHAR(50),
    amount     BIGINT,
    status     VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- REFUNDS
-- ============================================

CREATE TABLE refunds
(
    id         UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    amount     BIGINT,
    reason     TEXT,
    status     VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- WEBHOOK EVENTS
-- ============================================

CREATE TABLE webhook_events
(
    id         UUID PRIMARY KEY,
    provider   VARCHAR(50),
    event_type VARCHAR(100),
    payload    JSONB,
    processed  BOOLEAN   DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- IDEMPOTENCY KEYS
-- ============================================

CREATE TABLE idempotency_keys
(
    id              UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE,
    request_hash    VARCHAR(255),
    response        JSONB,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);