CREATE TABLE api_clients
(
    id          UUID PRIMARY KEY,
    client_code VARCHAR(80)  NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    api_key     VARCHAR(160) NOT NULL UNIQUE,
    webhook_url VARCHAR(255) NOT NULL,
    scopes      VARCHAR(255) NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE access_audits
(
    id          BIGSERIAL PRIMARY KEY,
    client_code VARCHAR(80)  NOT NULL,
    action      VARCHAR(80)  NOT NULL,
    outcome     VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_clients_status ON api_clients (status);
CREATE INDEX idx_access_audits_client_code ON access_audits (client_code);
