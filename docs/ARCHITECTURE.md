# Architecture Overview

## Intent

This repository models a fintech payment platform with a gateway edge and domain-specific services. The current implementation fully hardens the payment core and gives the surrounding services explicit bounded-context contracts so the repo reads like a realistic internal platform rather than a single-service demo.

## Services

- `api-gateway`
  Routes public traffic to backend services and exposes internal platform status routes for supporting services.
- `payment-service`
  Owns authentication endpoints, JWT/RBAC enforcement, order creation, payment initiation, payment capture, idempotency, audit logging, and PostgreSQL persistence.
- `auth-service`
  Represents the identity boundary and future token/credential ownership surface.
- `ledger-service`
  Represents accounting, posting, and reconciliation responsibilities.
- `notification-service`
  Represents customer and merchant notification orchestration.
- `risk-service`
  Represents risk scoring and fraud decisioning.
- `settlement-service`
  Represents clearing, payouts, and settlement batching.

## Package Strategy

The payment core follows a strict package split:

- `controller`
- `service`
- `domain`
- `repository`
- `config`
- `security`
- `dto`
- `exception`

This keeps delivery concerns separate from domain modeling and persistence, while still remaining approachable in a GitHub portfolio context.

## Core Payment Flow

1. User registers or logs in.
2. User creates an order.
3. User initiates a payment with an `Idempotency-Key`.
4. Service creates a provider-style order reference and a pending transaction.
5. User captures the payment with provider metadata.
6. Service marks the payment captured, updates the order lifecycle, and writes an audit log.
7. Admin users inspect orders, payments, and audit history through protected endpoints.

## Data Model

- `users`
- `roles`
- `user_roles`
- `orders`
- `payments`
- `transactions`
- `audit_logs`

The schema is managed directly by the application at startup for local development, with JPA mappings enforcing the core domain relationships and constraints.

## Local Runtime

- `docker-compose.yml` starts PostgreSQL plus all platform services.
- `api-gateway` is available at `:8080`.
- `payment-service` is available at `:8084`.
- Supporting service status routes are exposed through the gateway at `/platform/*/status`.
