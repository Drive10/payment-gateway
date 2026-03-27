create table payment_idempotency_records (
    id uuid primary key,
    operation varchar(40) not null,
    idempotency_key varchar(120) not null,
    request_hash varchar(64) not null,
    status varchar(20) not null,
    resource_id varchar(80),
    response_payload text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    unique (operation, idempotency_key)
);

create table payment_outbox_events (
    id uuid primary key,
    aggregate_type varchar(40) not null,
    aggregate_id varchar(80) not null,
    event_type varchar(80) not null,
    event_key varchar(120) not null,
    topic_name varchar(120) not null,
    payload text not null,
    correlation_id varchar(80),
    status varchar(20) not null,
    attempt_count integer not null default 0,
    next_attempt_at timestamp with time zone not null,
    published_at timestamp with time zone,
    last_error varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_payment_outbox_status_due on payment_outbox_events(status, next_attempt_at);
