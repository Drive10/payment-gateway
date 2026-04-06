# PayFlow Development Workflow

## Overview
This document describes the recommended development workflow for the PayFlow project on Apple Silicon (M1/M2) Macs.

## Infrastructure Services (Run in Docker)
The core infrastructure services should run in Docker containers for consistency:

```bash
# Start all infrastructure services
docker compose up mariadb mongodb redis onboarding -d

# Verify services are running:
# - MariaDB: localhost:3306 (database: payflow_db, user: root, password: rootpassword)
# - MongoDB: localhost:27017 (database: payflow_gateway)
# - Redis: localhost:6379
# - Onboarding Service: http://localhost:8081 (simple HTML placeholder)

# Stop services when done:
docker compose down
```

## Service Development (Run Locally on Host)
For optimal development experience on Apple Silicon, run each service locally on your Mac using your IDE (IntelliJ IDEA, VS Code, etc.):

### Prerequisites
- Java 21 (OpenJDK 21.0.10 or later)
- Maven 3.9+
- Running infrastructure services (from above)

### Running a Service Locally
Each service can be run directly from your IDE or command line:

```bash
# Example: Run the Auth Service locally
cd services/auth-service
mvn spring-boot:run -DskipTests
```

### Service Configuration for Local Development
When running services locally, configure them to connect to the Docker infrastructure:

#### Application Properties (src/main/resources/application.properties or application.yml)
```properties
# Database Connection
spring.datasource.url=jdbc:mariadb://localhost:3306/payflow_db
spring.datasource.username=root
spring.datasource.password=rootpassword

# MongoDB Connection
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=payflow_gateway

# Redis Connection
spring.redis.host=localhost
spring.redis.port=6379

# Server Port (adjust per service)
server.port=8081  # Auth Service
# server.port=8082  # API Gateway
# server.port=8083  # Payment Service
# etc.
```

### Available Services
You can run any of these services locally:
- `services/auth-service` (port 8081)
- `services/api-gateway` (port 8082) 
- `services/payment-service` (port 8083)
- `services/order-service` (port 8084)
- `services/notification-service` (port 8085)
- `services/audit-service` (port 8086)
- `services/analytics-service` (port 8087)
- `services/search-service` (port 8088)
- `services/simulator-service` (port 8089)
- `services/graphql-gateway` (port 8090)

## Development Workflow

### 1. Start Infrastructure
```bash
docker compose up mariadb mongodb redis onboarding -d
```

### 2. Develop Services Locally
- Open individual service folders in your IDE
- Run services directly from IDE or command line
- Services automatically connect to Docker infrastructure at localhost:3306/27017/6379
- Code changes take effect immediately (no container rebuild needed)

### 3. Test and Iterate
- Use HTTP clients (Postman, curl) to test endpoints
- Debug directly in your IDE
- View logs in IDE console

### 4. Stop When Done
```bash
docker compose down
```

## Why This Approach?
- ✅ **M1/M2 Optimized**: No Docker build/emulation overhead for Java services
- ✅ **Fast Development**: Instant code changes, no container rebuilds
- ✅ **Full Debugging**: Complete IDE debugging capabilities
- ✅ **Consistent Environment**: All services connect to same Docker infrastructure
- ✅ **Resource Efficient**: Only runs needed services locally
- ✅ **Simple Setup**: Avoids complex Maven/Docker build issues

## Optional: Containerized Service Testing
When you need to test a service in a container (closer to production):

```bash
# Build and run a specific service in Docker
mvn -pl services/api-gateway -am clean package -DskipTests
docker compose up --build api-gateway
```

But for day-to-day development, the local service approach is strongly recommended on Apple Silicon.

## Troubleshooting

### Port Conflicts
If you get port conflicts, stop any running services:
```bash
docker compose down
# Then restart only infrastructure
docker compose up mariadb mongodb redis onboarding -d
```

### Connection Issues
Verify infrastructure services are running:
```bash
docker ps
# Should show mariadb, mongodb, redis, onboarding containers
```

Test connections:
```bash
# Test MariaDB
mysql -h localhost -P 3306 -u root -prootpassword

# Test MongoDB
mongosh localhost:27017/payflow_gateway

# Test Redis
redis-cli -h localhost -p 6379 ping
```