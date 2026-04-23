# Payment Flows

## Card Flow
1. Client sends create-payment request with `Idempotency-Key`.
2. Payment service validates request and starts idempotency record (`IN_PROGRESS`).
3. Provider intent is created in simulator.
4. Payment transitions `PENDING -> CREATED -> AUTHORIZED`.
5. Outbox stores `payment.created` and `payment.authorized` events.
6. Capture request transitions `AUTHORIZED -> PROCESSING`.
7. Provider capture result:
- Success: `PROCESSING -> CAPTURED`, ledger posting, `payment.captured`.
- Failure: `PROCESSING -> FAILED`, `payment.failed`.
8. Outbox relay publishes events to Kafka with retries and DLQ fallback.

## UPI Collect Flow
1. Client sends `paymentMethod=UPI` in initiate API.
2. Payment intent created and moved to `AWAITING_UPI_PAYMENT`.
3. UPI deep link is generated and returned.
4. Provider callback arrives asynchronously.
5. Webhook dedupe checks `event_id`.
6. Status update:
- Success: `AWAITING_UPI_PAYMENT -> CAPTURED`.
- Timeout/no-action: expiration job marks `EXPIRED`.
- Failure callback: `FAILED`.
7. Payment events are emitted for downstream notification and analytics consumers.

## Idempotency Contract
- Scope key by operation + actor + idempotency key.
- Reuse with different payload returns conflict.
- Replay returns original response payload.
- Record lifecycle: `IN_PROGRESS -> COMPLETED`.
