# Architecture Review And Target Design

## Current Topology
- Edge traffic enters `api-gateway`.
- `payment-service` currently contains three bounded contexts in one deployable: `auth`, `order`, `payment`.
- Async events flow through Kafka with outbox relay in payment.
- Redis is used for idempotency cache and distributed relay locking.
- PostgreSQL is the system of record for payment state, transactions, idempotency records, and ledger.

## Data Ownership
- Payment domain: `payments`, `transactions`, `refunds`, `idempotency_records`, `payment_outbox_events`, `ledger_*`.
- Notification domain: webhook delivery states and channel dispatch history.
- Audit domain: compliance and security audit logs.
- Analytics domain: derived metrics and risk/settlement projections.

## Sync vs Async Communication
- Sync
  - Client -> Gateway -> Payment for intent/capture/refund APIs.
  - Payment -> Simulator for provider intent/capture simulation.
- Async
  - Payment outbox -> Kafka payment events.
  - Webhook updates consumed by payment listener for async status reconciliation.
  - Notification and analytics consumers subscribe to payment events.

## Transaction Boundaries
- ACID boundary: per-service database transaction.
- Cross-service consistency: event-based eventual consistency using outbox and retry.
- Failure strategy: retry with exponential backoff, then DLQ with audit event for manual intervention.

## Near-Term Boundary Fixes
1. Extract order/auth from payment service into separate deployables.
2. Replace direct order writes in webhook handlers with order-domain commands.
3. Add Flyway/Liquibase migration ownership per service.
4. Add explicit event schema compatibility checks in CI.
