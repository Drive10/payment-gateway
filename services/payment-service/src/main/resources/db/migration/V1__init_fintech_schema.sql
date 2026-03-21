CREATE TABLE roles
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE
);

CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    email      VARCHAR(120) NOT NULL UNIQUE,
    full_name  VARCHAR(120) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE orders
(
    id                 UUID PRIMARY KEY,
    user_id            BIGINT         NOT NULL REFERENCES users (id),
    order_reference    VARCHAR(40)    NOT NULL UNIQUE,
    external_reference VARCHAR(64)    NOT NULL UNIQUE,
    amount             NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency           VARCHAR(3)     NOT NULL,
    status             VARCHAR(32)    NOT NULL,
    description        VARCHAR(255)   NOT NULL,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payments
(
    id                  UUID PRIMARY KEY,
    order_id            UUID           NOT NULL REFERENCES orders (id),
    provider_order_id   VARCHAR(64)    NOT NULL UNIQUE,
    provider_payment_id VARCHAR(64) UNIQUE,
    idempotency_key     VARCHAR(120)   NOT NULL UNIQUE,
    amount              NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)     NOT NULL,
    provider            VARCHAR(32)    NOT NULL,
    method              VARCHAR(32)    NOT NULL,
    status              VARCHAR(32)    NOT NULL,
    provider_signature  VARCHAR(255),
    notes               VARCHAR(255),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions
(
    id                 UUID PRIMARY KEY,
    payment_id         UUID           NOT NULL REFERENCES payments (id),
    type               VARCHAR(32)    NOT NULL,
    status             VARCHAR(32)    NOT NULL,
    amount             NUMERIC(19, 2) NOT NULL CHECK (amount >= 0),
    provider_reference VARCHAR(64)    NOT NULL,
    remarks            VARCHAR(255),
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs
(
    id            BIGSERIAL PRIMARY KEY,
    action        VARCHAR(80)  NOT NULL,
    actor         VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80)  NOT NULL,
    resource_id   VARCHAR(80)  NOT NULL,
    summary       VARCHAR(512) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_orders_user_status ON orders (user_id, status);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);
CREATE INDEX idx_payments_order_status ON payments (order_id, status);
CREATE INDEX idx_payments_idempotency_key ON payments (idempotency_key);
CREATE INDEX idx_transactions_payment_id ON transactions (payment_id);
CREATE INDEX idx_audit_logs_actor_created_at ON audit_logs (actor, created_at DESC);
