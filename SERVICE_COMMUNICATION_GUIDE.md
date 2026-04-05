# Service Communication Guide for Payment Gateway

This document explains how service communication works in the payment gateway microservices architecture and how to configure it for different environments.

## Overview

The payment gateway uses a microservices architecture with:
- Multiple Spring Boot services (auth, order, payment, notification, etc.)
- An API Gateway (Spring Cloud Gateway) for routing and cross-cutting concerns
- A React frontend served via Vite dev server
- Docker Compose for container orchestration

## Communication Patterns

### 1. Frontend to API Gateway
- The frontend makes API calls to `/api/*` endpoints
- In development, Vite's dev server proxies these requests to the API gateway
- The target is configured via `VITE_API_GATEWAY_URL` environment variable

### 2. API Gateway to Services
- The API gateway routes requests to appropriate microservices
- Uses Spring Cloud Gateway with service-specific routes
- Service URLs are configured via environment variables (e.g., `AUTH_SERVICE_URL`)

### 3. Service-to-Service Communication
- Services communicate directly when needed (e.g., payment service calling order service)
- Uses REST calls via RestTemplate or WebClient
- Service URLs configured via environment variables

## Environment Configuration

### Local Development (Services Running Locally)
When running services locally with hot reload (using `./scripts/dev.sh up`):

1. Infrastructure (Postgres, Redis, Kafka, Vault) runs in Docker
2. Backend services run locally on your machine
3. Frontend runs locally via Vite dev server

Configuration:
- `.env` file sets service URLs to localhost (e.g., `AUTH_SERVICE_URL=http://localhost:8081`)
- Frontend `.env` sets `VITE_API_GATEWAY_URL=http://localhost:8080`
- API gateway routes to localhost services

### Docker Development (All Services in Docker)
When running everything in Docker (using `./scripts/dev.sh up --docker`):

1. All services run in Docker containers
2. Services communicate via Docker's internal network using service names as hostnames
3. Frontend still runs locally but proxies to API gateway in Docker

Configuration:
- `.env` file sets service URLs to Docker service names (e.g., `AUTH_SERVICE_URL=http://auth-service:8081`)
- Frontend `.env` overrides `VITE_API_GATEWAY_URL=http://api-gateway:8080`
- API gateway routes to Docker services using service names

## Key Improvements Made

1. **Frontend Proxy Configuration**:
   - Updated `vite.config.js` to use `VITE_API_GATEWAY_URL` environment variable
   - Added `.env` file with default value for local development
   - Modified `dev.sh` script to set appropriate URL based on mode

2. **API Gateway Service References**:
   - Changed default service URLs from `localhost:port` to Docker service names (e.g., `auth-service:8081`)
   - Updated `application-dev.yml` to use Docker service names for Redis and other services
   - Maintained backward compatibility with fallback to localhost values

3. **Environment Variable Management**:
   - Centralized service URLs in `.env` file
   - Created `.env.example` for documentation
   - Updated dev.sh script to handle frontend environment variables

## How to Use

### Local Development (Recommended for active development)
```bash
# Start infrastructure in Docker, services locally for hot reload
./scripts/dev.sh up

# This will:
# 1. Start Postgres, Redis, Kafka, Vault in Docker
# 2. Start all backend services locally (with hot reload)
# 3. Start frontend locally (proxying to localhost:8080)
```

### Docker Development (For testing production-like environment)
```bash
# Start everything in Docker
./scripts/dev.sh up --docker

# This will:
# 1. Start all infrastructure and services in Docker
# 2. Start frontend locally (proxying to api-gateway:8080 in Docker)
```

### Switching Between Modes
You can switch between modes by stopping and restarting with the appropriate flag:
```bash
# Switch from local to Docker mode
./scripts/dev.sh down
./scripts/dev.sh up --docker

# Switch from Docker to local mode
./scripts/dev.sh down
./scripts/dev.sh up
```

## Troubleshooting

### Frontend Can't Reach API Gateway
1. Check that the API gateway is running on the expected port
2. Verify the frontend proxy configuration:
   - For local mode: `VITE_API_GATEWAY_URL` should be `http://localhost:8080`
   - For Docker mode: `VITE_API_GATEWAY_URL` should be `http://api-gateway:8080`
3. Check browser console for proxy errors

### Services Can't Communicate With Each Other
1. Verify that services are on the same Docker network
2. Check that service URLs in `.env` use correct Docker service names
3. Check service logs for connection errors
4. Verify that dependent services are healthy (check with `./scripts/dev.sh status`)

### Common Issues
- **Port conflicts**: Make sure no other processes are using the same ports
- **DNS resolution**: In Docker, services must be referenced by their service names, not localhost
- **Environment variable precedence**: Docker Compose `.env` files override system environment variables

## Best Practices

1. **Keep environment-specific configuration separate**:
   - Use `.env` for environment-specific values
   - Check in `.env.example` as template
   - Never commit actual `.env` with secrets

2. **Use service names for inter-service communication in Docker**:
   - Docker provides automatic DNS resolution for service names
   - This works consistently across development and production

3. **Maintain parity between environments**:
   - Keep similar configuration patterns between local and Docker setups
   - This reduces "works on my machine" issues

4. **Leverage Docker's built-in features**:
   - Use Docker networks for service discovery
   - Use depends_on for startup ordering (though don't rely on it for health)
   - Use healthchecks to verify service readiness

## References

- Docker Compose Networking: https://docs.docker.com/compose/networking/
- Spring Cloud Gateway: https://cloud.spring.io/spring-cloud-gateway/
- Vite Proxy: https://vitejs.dev/config/server-options.html#server-proxy