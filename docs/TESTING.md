# Testing Guide

This repo now uses a layered test strategy so one broken path does not hide behind a single green command.

## Test layers

- Unit tests: fast logic checks for state machines, idempotency, JWT filters, outbox retry handling, and ledger rules
- Repository tests: database-level constraints such as actor-scoped idempotency uniqueness
- Spring integration tests: HTTP flows, security, OpenAPI contracts, and payment lifecycle behavior
- Cross-service integration tests: `payment-service -> Kafka -> ledger-service` journal propagation
- Docker-backed Testcontainers tests: heavier infrastructure validation that runs when Docker is available
- Frontend quality checks: lint plus production build validation

## Primary commands

- Fast backend smoke: `./mvnw -q -pl services/payment-service,services/ledger-service,services/api-gateway -am test`
- Full backend verify: `./mvnw -q verify`
- Full project matrix: `./scripts/dev.ps1 test-all` on Windows or `./scripts/dev.sh test-all` on macOS/Linux

## What `test-all` runs

- Compose rendering validation
- Full backend verify across modules
- Docker-backed Testcontainers coverage when Docker is available
- Frontend lint and production build when Node.js is available

## Coverage report

After `mvn verify`, the aggregated JaCoCo report is written to:

- `target/site/jacoco-aggregate/index.html`

## Practical expectation

No test suite can prove every imaginable edge case, but this matrix is designed to cover the main failure modes that matter in a payment platform:

- duplicate requests
- invalid state transitions
- event delivery failures
- ledger replay/idempotency issues
- auth and gateway enforcement regressions
- API contract drift
