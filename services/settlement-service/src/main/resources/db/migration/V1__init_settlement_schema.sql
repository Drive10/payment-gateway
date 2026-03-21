CREATE TABLE settlement_batches
(
    id UUID PRIMARY KEY,
    batch_reference VARCHAR(80) NOT NULL UNIQUE,
    total_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payout_instructions
(
    id UUID PRIMARY KEY,
    batch_reference VARCHAR(80) NOT NULL,
    beneficiary_account VARCHAR(80) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
