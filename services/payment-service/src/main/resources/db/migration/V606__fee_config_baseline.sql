-- Fee configuration table
CREATE TABLE fee_configs (
    id uuid primary key,
    merchant_id uuid not null unique,
    pricing_tier varchar(32) not null default 'STANDARD',
    platform_fee_percent numeric(5, 2) not null default 2.00,
    platform_fixed_fee numeric(19, 2) not null default 0,
    gateway_fee_percent numeric(5, 2) not null default 1.50,
    gateway_fixed_fee numeric(19, 2) not null default 2.00,
    volume_threshold numeric(19, 2),
    volume_discount_percent numeric(5, 2),
    min_fee numeric(19, 2) not null default 1.00,
    max_fee_percent numeric(5, 2) not null default 5.00,
    active boolean not null default true,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);

CREATE UNIQUE INDEX idx_fee_configs_merchant ON fee_configs(merchant_id);
