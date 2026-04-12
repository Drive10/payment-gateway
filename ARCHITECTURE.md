# PayFlow Production Architecture

This document outlines the transition from a monolithic repository to a distributed microservices ecosystem.

## Repository Structure

The system is split into five autonomous repositories:

1. **payflow-payment-core** - Consolidated backend (Auth, Order, Payment, Notification, Analytics, Audit)
2. **payflow-api-gateway** - Entry point with routing, security, and resilience
3. **payflow-simulator** - Testing sandbox for payment provider behavior
4. **payflow-payment-page** - Customer checkout UI
5. **payflow-dashboard** - Merchant administration portal

## Services

- **Payment Core**: Auth, Order, Payment, Notification, Analytics, Audit services
- **API Gateway**: Spring Cloud Gateway
- **Simulator**: Payment simulation service
- **Payment Page**: React + Vite checkout
- **Dashboard**: Next.js admin portal

## CI/CD Pipeline

Quality gates implemented:
1. **Lint** → Checkstyle, ESLint, OpenAPI validation
2. **Unit Tests** → Maven Surefire, Jest
3. **Integration Tests** → Testcontainers
4. **Security Scan** → Trivy, Snyk
5. **Build** → Maven package, npm build

## API Contracts

OpenAPI specifications are defined in `docs/api-specs/`:
- `payment-service.yaml`
- `auth-service.yaml`
- `order-service.yaml`
- `api-gateway.yaml`

## Contract Testing

Pact contract tests in `tests/contracts/`.

## Security

- JWT authentication
- Rate limiting (Redis)
- Circuit breaking (Resilience4j)
- mTLS for service-to-service communication
- Secrets management via environment variables