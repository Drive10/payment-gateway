# PayFlow Developer Guide

Welcome to PayFlow! This guide will help you get started with local development.

## Prerequisites

- Docker & Docker Compose
- Java 21 (for backend)
- Node.js 18+ & npm (for frontend)
- Maven 3.9+ (for backend builds)

## Quick Start

### 1. Clone and Setup

```bash
git clone https://github.com/Drive10/payflow.git
cd payflow

# Copy environment file (optional - defaults work out of box)
cp .env.example .env
```

### 2. Start Development

```bash
# Option A: Full stack (all services in Docker)
make up

# Option B: Development mode (infra + backend in Docker, frontend local)
make infra-up
make frontend-dev
```

### 3. Access Services

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Checkout UI |
| API Gateway | http://localhost:8080 | REST API entry |
| Auth Service | http://localhost:8081 | Authentication |
| Grafana | http://localhost:3001 | Monitoring (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics |

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
│                        Frontend (Port 3000)                  │
│  React + Vite → nginx (production)                          │
└────────────────────────────┬──────────────────────────────────┘
                             │
                      ┌──────▼──────┐
                      │ API Gateway  │
                      │   (8080)     │
                      └──────┬──────┘
                             │
    ┌────────────┬───────────┼───────────┬────────────┐
    ▼            ▼           ▼           ▼            ▼
┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐
│ Auth   │ │Payment│ │  Order   │ │ Notif    │ │Analytics│
│ 8081   │ │ 8083  │ │   8084   │ │   8085   │ │  8089   │
└────────┘ └────────┘ └──────────┘ └──────────┘ └─────────┘
    │            │           │            │            │
    └────────────┴───────────┴────────────┴────────────┘
                             │
              ┌──────────────┴──────────────┐
              ▼                            ▼
        ┌─────────────┐              ┌─────────────┐
        │  MariaDB   │              │    Redis    │
        │   (3306)   │              │   (6379)    │
        └─────────────┘              └─────────────┘
              │                            │
              └────────────┬───────────────┘
                           ▼
                   ┌─────────────┐
                   │   Kafka    │
                   │   (9092)   │
                   └─────────────┘
```

## Environment Variables

Create `.env` file with:

```bash
# Database
DB_ROOT_PASSWORD=rootpassword

# Redis
REDIS_PASSWORD=devpassword

# JWT (change in production!)
JWT_SECRET=your-secret-key
GATEWAY_INTERNAL_SECRET=internal-secret
```

## Service Endpoints

### Auth Service (8081)
- `POST /api/v1/auth/register` - Register user
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token

### Payment Service (8083)
- `POST /api/v1/payments` - Create payment
- `POST /api/v1/payments/{id}/capture` - Capture payment
- `POST /api/v1/payments/{id}/refunds` - Refund
- `GET /api/v1/payments` - List payments

### Order Service (8084)
- `POST /api/v1/orders` - Create order
- `GET /api/v1/orders` - List orders

## Testing Payment Flow

1. Open http://localhost:3000
2. Enter amount (e.g., 500)
3. Select "Test/Sandbox" mode
4. Enter test card: `4111 1111 1111 1111`, any future date, any CVV
5. Complete payment

## Troubleshooting

### Services won't start
```bash
make diagnose        # Run diagnostics
make down -v         # Clean restart
docker system prune  # Clean Docker
```

### Port conflicts
```bash
make ps              # Check running services
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

## Making Changes

### Backend (Java/Spring)
1. Make changes in `services/*/src/main/java/`
2. Rebuild: `docker compose build <service>`
3. Restart: `docker compose up -d <service>`

### Frontend (React)
1. Make changes in `web/frontend/src/`
2. Changes hot-reload with `make frontend-dev`

## Production Considerations

1. Change all default passwords in `.env`
2. Use strong JWT_SECRET (min 32 chars, base64)
3. Enable HTTPS
4. Configure proper domain names
5. Review security settings

## Need Help?

- Check logs: `make logs`
- Run diagnostics: `make diagnose`
- View service health: `make health`