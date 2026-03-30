create table api_clients (
    id uuid primary key,
    client_code varchar(80) not null unique,
    display_name varchar(120) not null,
    api_key varchar(160) not null unique,
    webhook_url varchar(255) not null,
    scopes varchar(255) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table access_audits (
    id bigserial primary key,
    client_code varchar(80) not null,
    action varchar(80) not null,
    outcome varchar(255) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
