create table simulation_transactions (
    id uuid primary key,
    order_reference varchar(64) not null,
    payment_reference varchar(64) not null,
    provider varchar(32) not null,
    provider_order_id varchar(80) not null unique,
    provider_payment_id varchar(80) unique,
    provider_signature varchar(255),
    simulation_mode varchar(16) not null,
    status varchar(16) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    checkout_url varchar(255) not null,
    notes varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
