# Contributing

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22+
- Docker Desktop or Docker Engine

## Local Development

Run the full platform:

```bash
cp .env.example .env
docker compose --profile services up --build
```

Run `payment-service` locally while the rest stays in Docker:

```bash
cp .env.example .env
docker compose --profile services up --build frontend postgres api-gateway auth-service ledger-service notification-service risk-service settlement-service simulator-service kafka prometheus grafana zipkin
```

Then start `payment-service` with:

```text
SPRING_PROFILES_ACTIVE=dev
```

## Test Commands

Backend:

```bash
mvn test
```

Payment integration with Testcontainers:

```bash
mvn -pl services/payment-service -Ptestcontainers -Dtest=PaymentFlowContainersIntegrationTest test
```

Frontend:

```bash
cd services/frontend
npm ci
npm run check
```

## Coding Guidelines

- Keep public APIs versioned under `/api/v1`.
- Use idempotency for write paths that can be retried.
- Prefer explicit domain transitions over implicit side effects.
- Do not reintroduce `ddl-auto:update` for non-test profiles.
- Preserve structured logs and correlation headers.
- Add tests for webhook, refund, and event-driven flows when behavior changes.

## Pull Request Expectations

- Green Maven tests
- Green frontend `npm run check`
- Updated docs for any externally visible behavior
- Clear commit messages
