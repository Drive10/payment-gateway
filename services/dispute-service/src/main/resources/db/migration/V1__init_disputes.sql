-- Disputes table
CREATE TABLE disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id VARCHAR(50) UNIQUE NOT NULL,
    payment_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(50),
    merchant_id UUID NOT NULL,
    customer_id VARCHAR(50),
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    dispute_reason VARCHAR(100) NOT NULL,
    dispute_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    due_date TIMESTAMP WITH TIME ZONE,
    evidence_deadline TIMESTAMP WITH TIME ZONE,
    resolution VARCHAR(50),
    resolution_notes TEXT,
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(100),
    chargeback_amount DECIMAL(19, 4),
    fee DECIMAL(19, 4) DEFAULT 0.0000,
    assigned_to VARCHAR(100),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_disputes_merchant ON disputes(merchant_id);
CREATE INDEX idx_disputes_payment ON disputes(payment_id);
CREATE INDEX idx_disputes_status ON disputes(status);
CREATE INDEX idx_disputes_reason ON disputes(dispute_reason);

-- Dispute Evidence table
CREATE TABLE dispute_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    evidence_type VARCHAR(50) NOT NULL,
    file_url VARCHAR(500),
    file_key VARCHAR(255),
    description TEXT,
    submitted_by VARCHAR(100),
    status VARCHAR(30) DEFAULT 'PENDING',
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by VARCHAR(100),
    review_notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dispute_evidence_dispute ON dispute_evidence(dispute_id);

-- Dispute Timeline table
CREATE TABLE dispute_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    event_description TEXT,
    actor VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dispute_timeline_dispute ON dispute_timeline(dispute_id);

-- Dispute Comments table
CREATE TABLE dispute_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dispute_id UUID NOT NULL REFERENCES disputes(id) ON DELETE CASCADE,
    comment TEXT NOT NULL,
    author VARCHAR(100) NOT NULL,
    author_type VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dispute_comments_dispute ON dispute_comments(dispute_id);
