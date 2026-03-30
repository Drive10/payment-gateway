create table settlement_batches (
    id uuid primary key,
    batch_reference varchar(80) not null unique,
    total_amount numeric(19, 2) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null
);

create table payout_instructions (
    id uuid primary key,
    batch_reference varchar(80) not null,
    beneficiary_account varchar(80) not null,
    amount numeric(19, 2) not null,
    status varchar(32) not null,
    created_at timestamp with time zone not null
);
