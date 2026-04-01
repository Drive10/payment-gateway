-- Restore idempotency constraints for payments and refunds
-- V4 removed these constraints, creating race condition vulnerability

-- Restore unique constraint on payments.idempotency_key
alter table payments
    add constraint uq_payments_idempotency_key unique (idempotency_key);

-- Restore unique constraint on payment_refunds.idempotency_key
alter table payment_refunds
    add constraint uq_payment_refunds_idempotency_key unique (idempotency_key);
