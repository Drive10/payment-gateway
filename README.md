# Cloud-Native Payment Gateway

[![Build](https://github.com/your-username/payment-gateway/actions/workflows/ci.yml/badge.svg)](./.github/workflows/ci.yml)
[![CodeQL](https://github.com/your-username/payment-gateway/actions/workflows/codeql.yml/badge.svg)](./.github/workflows/codeql.yml)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

Production-grade, modular payment gateway built with **Java 21**, **Spring Boot 3**, **Spring Cloud**, **Kafka**, **PostgreSQL**, **MongoDB**, **Redis**, **Docker**, **Kubernetes**, **Helm**, **Terraform**, and **GitHub Actions**.

> Last updated: 2025-08-13

## Highlights
- Domain-driven microservices: `payment-service`, `ledger-service`, `settlement-service`, `risk-service`, `notification-service`, `auth-service`, `api-gateway`.
- End-to-end security: TLS, OAuth2/JWT (Keycloak/OIDC), Secrets via Kubernetes and GitHub OIDC to AWS.
- Observability: OpenTelemetry, Prometheus, Grafana, structured logging.
- Deploy anywhere: Docker + Helm charts, K8s manifests, Terraform (AWS EKS), GitOps-ready.
- CI/CD: Build, test, SBOM, vulnerability scan (Trivy), CodeQL, container publish.

## Architecture

```mermaid
graph LR
    C[Client / Merchant] --> APIG[API Gateway]
    APIG --> AUTH[Auth Service]
    APIG --> PAY[Payment Service]
    PAY --> RISK[Risk Service]
    PAY --> LED[Ledger Service]
    PAY --> SETT[Settlement Service]
    PAY --> NOTIF[Notification Service]

    subgraph Data Stores
      PSQL[(PostgreSQL)]
      MONGO[(MongoDB)]
      REDIS[(Redis)]
    end

    PAY <--> PSQL
    LED <--> PSQL
    RISK <--> MONGO
    AUTH <--> PSQL
    NOTIF --> REDIS

    subgraph Messaging
      KAFKA[(Kafka Topics)]
    end
    PAY <--> KAFKA
    LED <--> KAFKA
    SETT <--> KAFKA

    subgraph Observability
      OTEL[OpenTelemetry]
      PROM[Prometheus]
      GRAF[Grafana]
    end
    PAY --> OTEL
    OTEL --> PROM
    PROM --> GRAF
```

## Quickstart (Local)

```bash
# 1) Build all modules
./mvnw -q -DskipTests=false clean verify

# 2) Start dependencies (dev): Kafka, Postgres, Redis, Mongo
docker compose -f deploy/docker-compose.dev.yml up -d

# 3) Run a service (example)
cd services/payment-service && ./mvnw spring-boot:run
```

> See `docs/LOCAL_DEV.md` for detailed instructions and sample `.env`.

## Kubernetes (Helm)

```bash
helm upgrade --install payment-gateway ./helm/payment-gateway   --namespace payments --create-namespace   -f helm/values/dev.yaml
```

## Repository Map
- `.github/workflows/` – CI/CD pipelines
- `docs/` – Guides (local dev, architecture, runbooks)
- `helm/` – Helm charts for services
- `k8s/` – Raw manifests (optional alternative to Helm)
- `infra/terraform/` – EKS cluster, RDS, MSK (Kafka) skeleton
- `adr/` – Architecture Decision Records

## Security
See [SECURITY.md](SECURITY.md). Report issues responsibly.

## License
[Apache-2.0](LICENSE)
