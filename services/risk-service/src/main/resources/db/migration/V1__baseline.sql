create table risk_assessments (
    id uuid primary key,
    merchant_reference varchar(80) not null,
    amount numeric(19, 2) not null,
    country_code varchar(8) not null,
    velocity_count integer not null,
    risk_score integer not null,
    decision varchar(32) not null,
    reasons varchar(255) not null,
    created_at timestamp with time zone not null
);
