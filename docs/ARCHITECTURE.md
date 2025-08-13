# Architecture

This repository demonstrates a microservice payment gateway with the following services:

- **api-gateway**: Edge routing, auth, rate-limits.
- **auth-service**: JWT issuance/validation, client credentials, token introspection.
- **payment-service**: Orchestration for payment flows (authorize, capture, refund).
- **ledger-service**: Double-entry ledger and settlements.
- **risk-service**: Fraud scoring and velocity checks.
- **settlement-service**: Payouts and reconciliation.
- **notification-service**: Webhooks, emails, provider callbacks.

Core components: Kafka for events, PostgreSQL for relational storage, MongoDB for document data, Redis for
caching/idempotency.

See README for the Mermaid diagram.