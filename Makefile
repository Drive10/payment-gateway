.PHONY: help build test clean up down logs status health check

help:
	@echo "PayFlow - Production-Grade Payment Gateway"
	@echo "============================================"
	@echo ""
	@echo "Infrastructure:"
	@echo "  make infra-up      - Start infrastructure (postgres, redis, kafka)"
	@echo "  make infra-down    - Stop infrastructure"
	@echo "  make infra-restart - Restart infrastructure"
	@echo ""
	@echo "Services:"
	@echo "  make gateway        - Run API Gateway (8080)"
	@echo "  make merchant      - Run Merchant Backend (8081)"
	@echo "  make payment       - Run Payment Service (8083)"
	@echo "  make notification  - Run Notification Service (8084)"
	@echo "  make simulator    - Run Simulator Service (8086)"
	@echo "  make frontend     - Run Frontend (5173)"
	@echo "  make all-services  - Run all services"
	@echo ""
	@echo "Development:"
	@echo "  make dev           - Start dev environment (infra + all services)"
	@echo "  make dev-merchant  - Run merchant backend only"
	@echo "  make logs          - View all logs"
	@echo "  make logs-svc      - View specific service logs (e.g., make logs-svc SVC=payment)"
	@echo ""
	@echo "Build:"
	@echo "  make build         - Build all JARs"
	@echo "  make build-frontend - Build frontend"
	@echo ""
	@echo "Testing:"
	@echo "  make test          - Run backend tests"
	@echo "  make test-frontend - Run frontend tests"
	@echo "  make test-all      - Run all tests"
	@echo "  make test-e2e      - Run E2E tests"
	@echo ""
	@echo "Utilities:"
	@echo "  make health        - Check service health"
	@echo "  make clean         - Clean all build artifacts"
	@echo "  make shell         - Open postgres shell"
	@echo ""
	@echo "Production:"
	@echo "  make docker-build  - Build Docker images"
	@echo "  make docker-up     - Start with Docker Compose"
	@echo ""

# =============================================================================
# Infrastructure
# =============================================================================

infra-up:
	@echo "Starting infrastructure..."
	docker compose up -d
	@echo "Waiting for services to be healthy..."
	@for i in 1 2 3 4 5 6 7 8 9 10; do \
		docker compose exec -T postgres pg_isready -U payflow > /dev/null 2>&1 && \
		docker compose exec -T redis redis-cli -a payflow ping > /dev/null 2>&1 && \
		echo "Infrastructure ready!" && exit 0; \
		echo "Waiting..."; \
		sleep 2; \
	done
	@echo "Timeout waiting for infrastructure"

infra-down:
	@echo "Stopping infrastructure..."
	docker compose down

infra-restart: infra-down infra-up

# =============================================================================
# Services
# =============================================================================

gateway:
	mvn spring-boot:run -pl src/api-gateway -Dspring-boot.run.profiles=local

merchant:
	mvn spring-boot:run -pl src/merchant-backend -Dspring-boot.run.profiles=local

payment:
	mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local

notification:
	mvn spring-boot:run -pl src/notification-service -Dspring-boot.run.profiles=local

simulator:
	mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local

analytics:
	mvn spring-boot:run -pl src/analytics-service -Dspring-boot.run.profiles=local

audit:
	mvn spring-boot:run -pl src/audit-service -Dspring-boot.run.profiles=local

frontend:
	cd frontend/payment-page && npm run dev

all-services:
	mvn spring-boot:run \
		-pl src/api-gateway,src/merchant-backend,src/payment-service,src/notification-service,src/simulator-service,src/audit-service,src/analytics-service

dev: infra-up all-services frontend

dev-payment: infra-up
	mvn spring-boot:run -pl src/payment-service -Dspring-boot.run.profiles=local

dev-merchant: infra-up
	mvn spring-boot:run -pl src/merchant-backend -Dspring-boot.run.profiles=local

dev-frontend:
	cd frontend/payment-page && npm run dev

dev-lite: 
	$(MAKE) infra-up
	$(MAKE) dev-payment & 
	$(MAKE) dev-frontend
	wait

dev-gateway: infra-up
	mvn spring-boot:run -pl src/api-gateway -Dspring-boot.run.profiles=local

dev-notification: infra-up
	mvn spring-boot:run -pl src/notification-service -Dspring-boot.run.profiles=local

dev-simulator: infra-up
	mvn spring-boot:run -pl src/simulator-service -Dspring-boot.run.profiles=local

# =============================================================================
# Logs
# =============================================================================

logs:
	docker compose logs -f --tail=100

logs-svc:
	@if [ -z "$(SVC)" ]; then \
		echo "Usage: make logs-svc SVC=payment"; \
		echo "Available services: postgres, redis, kafka, zookeeper"; \
		echo "Or custom: SVC=payment-service"; \
	else \
		docker compose logs -f $(SVC) --tail=100; \
	fi

# =============================================================================
# Build
# =============================================================================

build:
	@echo "Building all services..."
	mvn clean package -DskipTests
	@echo "Build complete!"

build-frontend:
	@echo "Building frontend..."
	cd frontend/payment-page && npm run build
	@echo "Frontend build complete!"

# =============================================================================
# Testing
# =============================================================================

test:
	@echo "Running backend tests..."
	mvn test
	@echo "Backend tests complete!"

test-backend:
	mvn test -pl src/payment-service,src/api-gateway,src/notification-service,src/simulator-service,src/audit-service,src/analytics-service -am

test-frontend:
	@echo "Running frontend tests..."
	cd frontend/payment-page && npm test -- --run
	@echo "Frontend tests complete!"

test-all: test test-frontend

test-e2e:
	@echo "Running E2E tests..."
	cd frontend/payment-page && npm run test:e2e
	@echo "E2E tests complete!"

test-coverage:
	@echo "Running tests with coverage..."
	mvn test jacoco:report
	cd frontend/payment-page && npm run test:coverage
	@echo "Coverage reports generated!"

# =============================================================================
# Utilities
# =============================================================================

health:
	@echo "Checking service health..."
	@echo ""
	@echo "Postgres: $$(docker compose exec -T postgres pg_isready -U payflow && echo 'UP' || echo 'DOWN')"
	@echo "Redis: $$(docker compose exec -T redis redis-cli -a payflow ping && echo 'UP' || echo 'DOWN')"
	@echo "Kafka: $$(docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 2>/dev/null && echo 'UP' || echo 'DOWN')"
	@echo ""
	@echo "Services: http://localhost:8080/actuator/health"

clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	cd frontend/payment-page && rm -rf dist node_modules/.vite coverage
	docker compose down -v --remove-orphans
	@echo "Clean complete!"

shell:
	docker compose exec postgres psql -U payflow -d payflow

# =============================================================================
# Docker
# =============================================================================

docker-build:
	@echo "Building Docker images..."
	docker compose build
	@echo "Docker images built!"

docker-up:
	@echo "Starting with Docker Compose..."
	docker compose up -d
	@echo "Services started!"
	@echo ""
	@echo "  Gateway:    http://localhost:8080"
	@echo "  Payment:    http://localhost:8083"
	@echo "  Frontend:   http://localhost:5173"
	@echo "  Swagger:    http://localhost:8083/swagger-ui.html"

# =============================================================================
# Payment Flow Testing
# =============================================================================

test-payment-card:
	@echo "Testing card payment flow..."
	curl -X POST http://localhost:8083/api/v1/payments \
		-H "Content-Type: application/json" \
		-H "Idempotency-Key: $$(uuidgen)" \
		-d '{"orderId":"00000000-0000-0000-0000-000000000001","merchantId":"00000000-0000-0000-0000-000000000002","method":"CARD","provider":"RAZORPAY_SIMULATOR","transactionMode":"TEST","notes":"makefile card flow"}'

test-payment-upi:
	@echo "Testing UPI payment flow..."
	curl -X POST http://localhost:8083/api/v1/payments \
		-H "Content-Type: application/json" \
		-H "Idempotency-Key: $$(uuidgen)" \
		-d '{"orderId":"00000000-0000-0000-0000-000000000001","merchantId":"00000000-0000-0000-0000-000000000002","method":"UPI","provider":"RAZORPAY_SIMULATOR","transactionMode":"TEST","notes":"makefile upi flow"}'

test-payment-simulator:
	@echo "Testing simulator..."
	curl -X POST http://localhost:8086/api/v1/simulate/payment \
		-H "Content-Type: application/json" \
		-d '{"amount": 100, "currency": "INR", "testMode": "SUCCESS"}'
