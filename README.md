# PayFlow — Enterprise Payment Gateway

> A production-grade, cloud-native payment processing platform built with Spring Boot 3, Kafka, PostgreSQL, and React. Designed for high-throughput, low-latency payment orchestration with real-time analytics, multi-currency support, and enterprise-grade security.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-3.7-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=black)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client Layer                               │
│  ┌──────────────────┐              ┌──────────────────────────┐    │
│  │   Checkout UI    │              │   Admin Dashboard (React) │    │
│  │  (web/frontend)  │              │   (web/dashboard)         │    │
│  └────────┬─────────┘              └────────────┬─────────────┘    │
└───────────┼─────────────────────────────────────┼──────────────────┘
            │                                     │
            ▼                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (Spring Cloud)                   │
│  ┌─────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────────┐ │
│  │ JWT Auth    │ │ Rate Limiting│ │ CORS Policy  │ │ Request    │ │
│  │ Validation  │ │ (Redis)      │ │ Security     │ │ Validation │ │
│  └─────────────┘ └──────────────┘ └──────────────┘ └────────────┘ │
└───────────────────────────┬───────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│ Auth Service │  │Order Service │  │ Payment Service  │
│              │  │              │  │                  │
│ • JWT Auth   │  │ • Orders     │  │ • Stripe/Razorpay│
│ • OAuth2     │  │ • Merchants  │  │ • Webhooks       │
│ • RBAC       │  │ • API Keys   │  │ • Settlements    │
│ • Sessions   │  │ • KYC        │  │ • Disputes       │
└──────┬───────┘  └──────┬───────┘  └────────┬─────────┘
       │                 │                    │
       ▼                 ▼                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Event-Driven Messaging (Kafka)                  │
│  order.events │ payment.events │ webhook.updates │ payment.status   │
└───────────────────────────┬─────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│ Notification │  │ Analytics    │  │ Simulator Service│
│ Service      │  │ Service      │  │                  │
│              │  │              │  │ • Payment Sim    │
│ • Email/SMS  │  │ • Risk Score │  │ • Load Testing   │
│ • Push       │  │ • Settlements│  │ • Demo Mode      │
│ • Webhooks   │  │ • Disputes   │  │ • Mock Providers │
│ • Flags      │  │ • Reports    │  │                  │
└──────────────┘  └──────────────┘  └──────────────────┘
```

## Services

| Service | Port | Description | Tech |
|---------|------|-------------|------|
| **api-gateway** | 8080 | Central routing, auth, rate limiting | Spring Cloud Gateway |
| **auth-service** | 8081 | JWT auth, OAuth2, RBAC, sessions | Spring Security |
| **order-service** | 8082 | Order management, merchants, KYC, API keys | Spring Data JPA |
| **payment-service** | 8083 | Payment orchestration, Stripe, Razorpay | Spring Boot |
| **notification-service** | 8084 | Email, SMS, push, webhooks, feature flags | Kafka, Redis |
| **analytics-service** | 8085 | Risk scoring, settlements, disputes, reports | Kafka, JPA |
| **simulator-service** | 8086 | Payment simulation, load testing, demo mode | Spring Boot |

## Features

### Payment Processing
- **Multi-Provider**: Stripe, Razorpay, PayPal integration
- **Multi-Currency**: 150+ currencies with real-time conversion
- **Smart Routing**: Automatic provider selection based on cost/success rate
- **Idempotency**: Guaranteed exactly-once processing
- **Retry Logic**: Exponential backoff with circuit breaker

### Security
- **JWT Authentication**: HS512 signed tokens with refresh flow
- **RBAC**: Admin, Merchant, User roles with method-level security
- **Rate Limiting**: Token bucket algorithm via Redis
- **API Keys**: HMAC-signed keys for merchant integrations
- **PCI DSS**: Card data never touches our servers (tokenization)
- **Secrets Management**: Zero hardcoded secrets

### Real-Time
- **Event Streaming**: Kafka for all payment state changes
- **WebSocket**: Live payment status updates to dashboard
- **Webhooks**: Configurable event delivery to merchant endpoints
- **Feature Flags**: Runtime toggling without restarts

### Analytics & Monitoring
- **Risk Scoring**: Real-time fraud detection
- **Settlement Engine**: Automated merchant payouts
- **Dispute Management**: Chargeback tracking and resolution
- **Revenue Reports**: Multi-dimensional analytics

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Node.js 22+ (for web apps)
- Maven 3.9+

### Run with Docker (Recommended)
```bash
# Start all infrastructure + services
docker compose up -d

# Check service health
docker compose ps

# View logs
docker compose logs -f
```

### Run in Dev Mode (Hot Reload)
```bash
# Start infrastructure only
docker compose --profile infra up -d

# Run services locally with Maven
./scripts/dev.sh start

# Start web apps
cd web/frontend && npm run dev
cd web/dashboard && npm run dev
```

### Build
```bash
# Build all services
mvn clean package -DskipTests

# Build a single service
mvn clean package -pl services/payment-service -DskipTests
```

## API Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"demo@payflow.dev","password":"Demo@1234","firstName":"Demo","lastName":"User"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@payflow.com","password":"Test@1234"}'

# Create Order (requires auth token)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"amount":5000,"currency":"USD","customerEmail":"test@example.com","description":"Test Order"}'

# Create Payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"orderId":"<order-id>","provider":"STRIPE","paymentMethod":"CARD"}'
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 21, Spring Boot 3.3, Spring Cloud 2023 |
| **Database** | PostgreSQL 16, Flyway migrations |
| **Messaging** | Apache Kafka 3.7 |
| **Cache** | Redis 7 |
| **Frontend** | React 19, TypeScript, Tailwind CSS, Vite |
| **Gateway** | Spring Cloud Gateway (WebFlux) |
| **Security** | Spring Security, JWT (JJWT), BCrypt |
| **Resilience** | Resilience4j (Circuit Breaker, Retry) |
| **Observability** | OpenTelemetry, Prometheus, Grafana |
| **Container** | Docker, Docker Compose |
| **Build** | Maven, Make |

## Project Structure

```
payment-gateway/
├── libs/common/              # Shared library (DTOs, exceptions, utils)
├── services/
│   ├── api-gateway/          # Central API gateway
│   ├── auth-service/         # Authentication & authorization
│   ├── order-service/        # Orders, merchants, KYC, API keys
│   ├── payment-service/      # Payment orchestration
│   ├── notification-service/ # Notifications, webhooks, feature flags
│   ├── analytics-service/    # Risk, settlements, disputes, reports
│   └── simulator-service/    # Payment simulation & testing
├── web/
│   ├── dashboard/            # Admin & merchant dashboard (React)
│   └── frontend/             # Customer checkout (React)
├── docker/
│   ├── postgres/init/        # Database initialization
│   ├── vault/                # Vault TLS & init scripts
│   ├── redis/tls/            # Redis TLS certificates
│   └── kafka/tls/            # Kafka TLS certificates
├── docs/                     # Architecture & security docs
├── chaos/                    # Chaos engineering tests
└── scripts/                  # Development scripts
```

## Development

```bash
# Build all services
mvn clean package -DskipTests

# Start infrastructure (Postgres, Kafka, Redis, Vault)
docker compose --profile infra up -d

# Run all backend services with hot reload
./scripts/dev.sh start

# Start web applications
cd web/frontend && npm run dev

# Check service health
./scripts/dev.sh status

# View logs
./scripts/dev.sh logs

# Stop everything
./scripts/dev.sh stop
```

## CI/CD

This project uses GitHub Actions for automated builds and testing:

- **Build**: Compiles all services and builds Docker images
- **Test**: Runs unit tests with PostgreSQL and Redis
- **Security**: Trivy vulnerability scanning and hardcoded secret detection
- **Deploy**: Pushes images to GitHub Container Registry

See [`.github/workflows/build.yml`](.github/workflows/build.yml) for the pipeline configuration.

## Security

This project follows OWASP Top 10 guidelines and implements:
- JWT-based authentication with secure token management
- Rate limiting on all API endpoints
- Input validation and sanitization
- SQL injection prevention via parameterized queries
- XSS protection through security headers
- Secure password hashing with bcrypt
- Regular dependency scanning via Dependabot & CodeQL

See [SECURITY.md](SECURITY.md) for the complete security policy and [docs/SECURITY-CHECKLIST.md](docs/SECURITY-CHECKLIST.md) for the implementation checklist.

## License

MIT License — see [LICENSE](LICENSE)

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.
