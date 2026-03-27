create table roles (
    id bigserial primary key,
    name varchar(32) not null unique
);

create table users (
    id bigserial primary key,
    email varchar(120) not null unique,
    full_name varchar(120) not null,
    password varchar(255) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table user_roles (
    user_id bigint not null references users(id),
    role_id bigint not null references roles(id),
    primary key (user_id, role_id)
);

create table orders (
    id uuid primary key,
    order_reference varchar(40) not null unique,
    external_reference varchar(64) not null unique,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    status varchar(32) not null,
    description varchar(255) not null,
    user_id bigint not null references users(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table payments (
    id uuid primary key,
    order_id uuid not null references orders(id),
    provider_order_id varchar(64) not null unique,
    provider_payment_id varchar(64) unique,
    idempotency_key varchar(120) not null unique,
    amount numeric(19, 2) not null,
    refunded_amount numeric(19, 2) not null,
    currency varchar(3) not null,
    provider varchar(32) not null,
    method varchar(32) not null,
    status varchar(32) not null,
    transaction_mode varchar(16) not null,
    simulated boolean not null,
    provider_signature varchar(255),
    checkout_url varchar(255) not null,
    notes varchar(255),
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table transactions (
    id uuid primary key,
    payment_id uuid not null references payments(id),
    type varchar(32) not null,
    status varchar(32) not null,
    amount numeric(19, 2) not null,
    provider_reference varchar(64) not null,
    remarks varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table payment_refunds (
    id uuid primary key,
    payment_id uuid not null references payments(id),
    idempotency_key varchar(120) not null unique,
    refund_reference varchar(80) not null unique,
    provider_refund_id varchar(80) unique,
    amount numeric(19, 2) not null,
    reason varchar(255),
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table processed_webhook_events (
    id uuid primary key,
    event_id varchar(120) not null unique,
    event_type varchar(80) not null,
    signature varchar(128) not null,
    payload_hash varchar(64) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table audit_logs (
    id bigserial primary key,
    action varchar(80) not null,
    actor varchar(120) not null,
    resource_type varchar(80) not null,
    resource_id varchar(80) not null,
    summary varchar(512) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_orders_user_id on orders(user_id);
create index idx_payments_order_id on payments(order_id);
create index idx_transactions_payment_id on transactions(payment_id);
create index idx_payment_refunds_payment_id on payment_refunds(payment_id);
create index idx_audit_logs_resource on audit_logs(resource_type, resource_id);
