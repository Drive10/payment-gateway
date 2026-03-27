create table templates (
    id uuid primary key,
    template_code varchar(64) not null unique,
    channel varchar(32) not null,
    subject varchar(120) not null,
    body varchar(2048) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table notifications (
    id uuid primary key,
    recipient_address varchar(255) not null,
    template_code varchar(64) not null,
    channel varchar(32) not null,
    status varchar(32) not null,
    payload varchar(2048) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table consumed_events (
    id uuid primary key,
    consumer_name varchar(80) not null,
    event_id varchar(80) not null unique,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
