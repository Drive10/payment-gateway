# Cloud-Native Payment Gateway

[![CI](https://github.com/your-user/your-repo/actions/workflows/ci.yml/badge.svg)](https://github.com/your-user/your-repo/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-007396?logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)
![Kafka](https://img.shields.io/badge/Kafka-Event%20Driven-231F20?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-DB-336791?logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-Doc-47A248?logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-Cache-DC382D?logo=redis)
![Docker](https://img.shields.io/badge/Docker-Container-2496ED?logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-326CE5?logo=kubernetes)

Production-grade, microservices-based payment gateway built with **Java 21**, **Spring Boot 3**, **Spring Cloud**, **Kafka**, **PostgreSQL**, **MongoDB**, **Redis**, and deployable to **Docker/Kubernetes**.  
Default profile is **`local`**; future `prod` profile can be added easily.

---

## Quick Start (Local)

### 1) Prereqs
- Java 21, Docker, Docker Compose
- IntelliJ IDEA (recommended)

### 2) Start infra & dependencies
```bash
docker compose -f deploy/local/docker-compose.yml up -d
```

### 3) Run services (IntelliJ)
Open the project, and run each Spring Boot application. No profile flag required — `local` is the default.

### 4) Smoke check
- API Gateway: http://localhost:8080 (adjust if different)
- Payment Service: http://localhost:8081/actuator/health

---

## Architecture

```mermaid
flowchart LR
  subgraph Gateway
    APIGW[API Gateway]
  end
  subgraph Services
    AUTH[Auth Service]
    PAY[Payment Service]
    LEDGER[Ledger Service]
    RISK[Risk Service]
    SETTLE[Settlement Service]
    NOTIF[Notification Service]
  end
  subgraph Data
    PG[(PostgreSQL)]
    MG[(MongoDB)]
    RD[(Redis)]
    KF[(Kafka)]
  end

  APIGW --> AUTH
  APIGW --> PAY
  PAY --> LEDGER
  PAY --> RISK
  PAY --> SETTLE
  PAY --> NOTIF
  PAY <--> KF
  LEDGER --> PG
  RISK --> MG
  NOTIF --> KF
  PAY --> RD
```

- See **docs/ARCHITECTURE.md** for service responsibilities and flows.
- Observability, security, and deployment guides are in `docs/`.

---

## CI/CD (GitHub Actions)
- Build & test on PRs
- On tags, build & push Docker images to GHCR (`ghcr.io`)

---

## Security
- OAuth2/JWT between services
- TLS termination at gateway/ingress
- Secrets via environment or Vault (recommended in prod)

---

## Observability
- OpenTelemetry instrumentation for traces
- Prometheus scraping for metrics
- Grafana dashboards (starter JSON in `docs/observability/`)

---

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## License
[MIT](LICENSE)