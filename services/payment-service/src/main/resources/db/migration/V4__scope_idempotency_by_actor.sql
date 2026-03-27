alter table payment_idempotency_records
    add column if not exists actor_id bigint not null default 0;

alter table payment_idempotency_records
    drop constraint if exists uk_payment_idempotency_operation_key;

alter table payment_idempotency_records
    drop constraint if exists payment_idempotency_records_operation_idempotency_key_key;

alter table payment_idempotency_records
    add constraint uk_payment_idempotency_operation_actor_key unique (operation, actor_id, idempotency_key);

alter table payments
    drop constraint if exists payments_idempotency_key_key;

alter table payment_refunds
    drop constraint if exists payment_refunds_idempotency_key_key;
