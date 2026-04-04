-- API Keys table
CREATE TABLE api_keys (
    id uuid primary key,
    merchant_id uuid not null,
    key_prefix varchar(16) not null,
    key_hash varchar(255) not null unique,
    name varchar(120) not null,
    description varchar(512),
    status varchar(20) not null default 'ACTIVE',
    permissions text,
    rate_limit_per_minute integer default 100,
    rate_limit_per_day integer default 10000,
    ip_whitelist text,
    referer_whitelist text,
    last_used_at timestamp with time zone,
    last_used_ip varchar(45),
    expires_at timestamp with time zone,
    rotated_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

CREATE INDEX idx_api_keys_merchant ON api_keys(merchant_id);
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_status ON api_keys(status);
