-- Disputes table
CREATE TABLE disputes (
    id uuid primary key,
    dispute_reference varchar(50) not null unique,
    payment_id uuid not null,
    merchant_id uuid not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null default 'INR',
    status varchar(32) not null,
    reason varchar(32) not null,
    description varchar(500),
    customer_email varchar(120),
    customer_name varchar(120),
    chargeback_amount numeric(19, 2),
    chargeback_currency varchar(3),
    initiated_by varchar(50),
    initiated_at timestamp with time zone,
    due_by timestamp with time zone,
    won_at timestamp with time zone,
    lost_at timestamp with time zone,
    closed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

CREATE INDEX idx_disputes_merchant ON disputes(merchant_id);
CREATE INDEX idx_disputes_payment ON disputes(payment_id);
CREATE INDEX idx_disputes_reference ON disputes(dispute_reference);
CREATE INDEX idx_disputes_status ON disputes(status);

-- Dispute evidence table
CREATE TABLE dispute_evidence (
    id uuid primary key,
    dispute_id uuid not null,
    evidence_type varchar(50) not null,
    file_url varchar(500),
    file_key varchar(255),
    description text,
    submitted_by varchar(120),
    submitted_at timestamp with time zone not null,
    accepted boolean not null default false,
    review_notes varchar(500),
    reviewed_by varchar(120),
    reviewed_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

CREATE INDEX idx_dispute_evidence_dispute ON dispute_evidence(dispute_id);
