# PayFlow Developer Guide

Welcome to PayFlow! This guide will help you get started with local development.

## Prerequisites

- Docker & Docker Compose
- Java 21+ (for backend)
- Node.js 22+ & npm (for frontend)
- Maven 3.9+ (for backend builds)

## Quick Start

### 1. Clone and Setup

```bash
git clone https://github.com/Drive10/payflow.git
cd payflow
```

### 2. Start Development

```bash
# Option A: Full stack (all services in Docker)
docker compose up -d

# Option B: Development mode (infra + backend in Docker, frontend local)
docker compose --profile infra up -d
make frontend-dev
```

### 3. Access Services

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:5173 | Checkout UI |
| API Gateway | http://localhost:8080 | REST API entry |
| GraphQL | http://localhost:8087/graphiql | GraphQL Playground |
| Grafana | http://localhost:3000 | Monitoring (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics |
| Jaeger | http://localhost:16686 | Distributed Tracing |

## Common Commands

```bash
# Development
make dev              # Clean start (down-v + up)
make rebuild          # Rebuild all services
make diagnose         # Run diagnostics

# Logs
make logs             # All logs
make logs SERVICE=payment-service  # Specific service

# Testing
make test             # Run all tests
make test-backend     # Backend only
make test-frontend    # Frontend only

# Code Quality
make lint             # Run linters
make format           # Format code

# Cleanup
make down             # Stop all
make down-v           # Stop + remove volumes
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (Port 5173)                    │
│               React + Vite → nginx (production)             │
└────────────────────────────┬──────────────────────────────────┘
                             │
                    ┌────────▼────────┐
                    │  API Gateway    │
                    │    (8080)       │
                    └────────┬────────┘
                             │
     ┌────────────┬─────────┼─────────┬────────────┐
     ▼            ▼         ▼         ▼            ▼
┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐
│  Auth  │ │ Order  │ │ Payment  │ │  Notif   │ │Analytics│
│ 8081   │ │ 8082   │ │   8083   │ │  8084    │ │  8089   │
└────────┘ └────────┘ └──────────┘ └──────────┘ └─────────┘
     │            │         │          │            │
     └────────────┴─────────┼─────────┴────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
    ┌───────────┐    ┌─────────────┐    ┌───────────┐
    │ PostgreSQL│    │   MongoDB   │    │   Redis   │
    │   (5432)  │    │   (27017)   │    │  (6379)   │
    └───────────┘    └─────────────┘    └───────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                    ┌────────▼────────┐
                    │     Kafka       │
                    │    (9092)       │
                    └─────────────────┘
                             │
          ┌──────────────────┼──────────────────┐
          ▼                  ▼                  ▼
   ┌─────────────┐  ┌─────────────┐   ┌─────────────┐
   │Notification │  │  Analytics  │   │   Search    │
   │  Service    │  │  Service    │   │  Service    │
   └─────────────┘  └─────────────┘   └─────────────┘
```

## Service Endpoints

### API Gateway (8080)
- All REST API routes via `/api/v1/*`
- Health: `GET /actuator/health`

### Auth Service (8081)
- `POST /api/v1/auth/register` - Register user
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Current user

### Order Service (8082)
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders` - List orders
- `GET /api/v1/orders/{id}` - Get order
- `PUT /api/v1/orders/{id}` - Update order

### Payment Service (8083)
- `POST /api/v1/payments` - Create payment
- `POST /api/v1/payments/{id}/capture` - Capture payment
- `POST /api/v1/payments/{id}/refunds` - Refund
- `GET /api/v1/payments` - List payments
- `GET /api/v1/payments/{id}` - Get payment

### Notification Service (8084)
- `POST /api/v1/notifications/send` - Send notification
- `GET /api/v1/webhooks` - List webhooks
- `POST /api/v1/webhooks` - Register webhook

### GraphQL Gateway (8087)
- Playground: `http://localhost:8087/graphiql`
- Endpoint: `http://localhost:8087/graphql`

### Search Service (8088)
- `GET /api/v1/search/payments` - Search payments
- `GET /api/v1/search/orders` - Search orders

### Analytics Service (8089)
- `GET /api/v1/analytics/summary` - Analytics summary
- `GET /api/v1/analytics/reports` - Generate reports

### Audit Service (8089)
- MongoDB-based audit logging
- Event sourcing support

## Environment Variables

Create `.env` file with:

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=payflow
DB_USERNAME=payflow
DB_PASSWORD=payflow

# MongoDB
MONGO_HOST=localhost
MONGO_PORT=27017
MONGO_DB=audit

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# JWT (change in production!)
JWT_SECRET=your-secret-key-min-32-chars
JWT_EXPIRATION=3600000

# Payment Providers
STRIPE_API_KEY=sk_test_xxx
RAZORPAY_KEY_ID=xxx
RAZORPAY_KEY_SECRET=xxx
```

## Testing Payment Flow

1. Open http://localhost:5173
2. Enter amount (e.g., 500)
3. Select "Test/Sandbox" mode
4. Enter test card: `4111 1111 1111 1111`, any future date, CVV: 123
5. Complete payment
6. Download PDF receipt

## Troubleshooting

### Services won't start
```bash
make diagnose        # Run diagnostics
make down -v         # Clean restart
docker system prune  # Clean Docker
```

### Port conflicts
```bash
docker compose ps    # Check running services
lsof -i :8080        # Find what's using a port
```

### Database issues
```bash
make db-reset        # Reset databases
```

### View logs
```bash
make logs            # All services
make logs SERVICE=auth-service  # Specific
```

### Health checks
```bash
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Auth Service
```

## Making Changes

### Backend (Java/Spring)
1. Make changes in `services/*/src/main/java/`
2. Rebuild: `docker compose build <service>`
3. Restart: `docker compose up -d <service>`

### Frontend (React)
1. Make changes in `web/frontend/src/`
2. Changes hot-reload with `npm run dev` in web/frontend

## Production Considerations

1. Change all default passwords in `.env`
2. Use strong JWT_SECRET (min 32 chars, base64)
3. Enable HTTPS/TLS
4. Configure proper domain names
5. Review security settings in SECURITY.md
6. Set up proper monitoring and alerting
7. Configure backup strategies for databases

## Need Help?

- Check logs: `make logs`
- Run diagnostics: `make diagnose`
- View service health: `make health`
- Check README.md for full architecture
- See CONTRIBUTING.md for development workflow