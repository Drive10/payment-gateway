# Contracts

## API Contracts

- Public APIs are versioned under `/api/v1`.
- Each service owns its own request and response DTOs under its local `dto` package.
- OpenAPI is the source of truth for HTTP contracts. Current documented services:
  - `payment-service`
  - `auth-service`

## Event Contracts

`payment-service` publishes a versioned `PaymentEventMessage` contract for Kafka.

Current event envelope fields:

- `eventId`
- `eventVersion`
- `eventType`
- `paymentId`
- `orderId`
- `orderReference`
- `provider`
- `paymentStatus`
- `transactionMode`
- `simulated`
- `amount`
- `refundedAmount`
- `currency`
- `occurredAt`
- `metadata`
- `correlationId`

## Boundary Rule

Shared code in `libs/common` should stay limited to:

- edge response envelopes
- pagination helpers
- cross-service event contracts

Business DTOs and domain models should remain private to each service so versioning can evolve independently.
