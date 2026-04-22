.PHONY: help build test clean up down logs infra

help:
	@echo "PayFlow Development Commands"
	@echo "========================="
	@echo "  make infra      - Start infrastructure (postgres, redis, kafka)"
	@echo "  make up        - Start all services"
	@echo "  make down      - Stop infrastructure"
	@echo "  make logs      - View logs"
	@echo "  make build    - Build all JARs"
	@echo "  make test     - Run tests"
	@echo "  make clean   - Clean build artifacts"
	@echo ""
	@echo "Service Commands:"
	@echo "  make auth       - Run auth-service"
	@echo "  make order     - Run order-service"
	@echo "  make payment   - Run payment-service"
	@echo "  make frontend  - Run frontend"

infra:
	docker compose up -d

up: infra
	mvn spring-boot:run -pl src/api-gateway,src/auth-service,src/order-service,src/payment-service,src/notification-service,src/simulator-service

down:
	docker compose down

logs:
	docker compose logs -f

build:
	mvn clean package -DskipTests

test:
	mvn test -pl src/payment-service
	cd frontend/payment-page && npm test

clean:
	mvn clean
	cd frontend/payment-page && rm -rf dist node_modules/.vite

auth:
	mvn spring-boot:run -pl src/auth-service

order:
	mvn spring-boot:run -pl src/order-service

payment:
	mvn spring-boot:run -pl src/payment-service

notification:
	mvn spring-boot:run -pl src/notification-service

simulator:
	mvn spring-boot:run -pl src/simulator-service

gateway:
	mvn spring-boot:run -pl src/api-gateway

frontend:
	cd frontend/payment-page && npm run dev