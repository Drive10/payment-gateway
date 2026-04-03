-- Re-add unique constraints for idempotency to prevent duplicates in production
ALTER TABLE payments ADD CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key);
ALTER TABLE payment_refunds ADD CONSTRAINT uq_payment_refunds_idempotency_key UNIQUE (idempotency_key);
