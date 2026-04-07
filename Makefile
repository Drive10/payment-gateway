.PHONY: help infra-up infra-down infra-logs up down down-v clean build build-all rebuild logs ps test lint format frontend-build frontend-dev hybrid-backend hybrid-frontend db-reset

help:
	@echo "PayFlow - Makefile Commands"
	@echo ""
	@echo "Infrastructure:"
	@echo "  make infra-up        Start infrastructure (MariaDB, MongoDB, Redis, Kafka, Zookeeper)"
	@echo "  make infra-down      Stop infrastructure"
	@echo "  make infra-logs      View infrastructure logs"
	@echo ""
	@echo "Full Stack:"
	@echo "  make up              Start all services (full Docker stack)"
	@echo "  make down            Stop all services"
	@echo "  make down-v          Stop all services + remove volumes (fresh start)"
	@echo "  make rebuild         Rebuild and restart all services"
	@echo ""
	@echo "Hybrid Mode:"
	@echo "  make hybrid-backend  Start infra + backend in Docker, run frontend locally"
	@echo "  make hybrid-frontend Start infra in Docker, run backend locally + frontend locally"
	@echo ""
	@echo "Build:"
	@echo "  make build-all       Build all Docker images"
	@echo "  make build SERVICE=x Build a single service (e.g. make build SERVICE=auth-service)"
	@echo ""
	@echo "Development:"
	@echo "  make logs            View all service logs"
	@echo "  make logs SERVICE=x  View logs for a specific service"
	@echo "  make ps              List running containers"
	@echo "  make frontend-dev    Run frontend dev server (npm run dev)"
	@echo "  make frontend-build  Build frontend for production"
	@echo ""
	@echo "Database:"
	@echo "  make db-reset        Reset all database volumes (fresh data)"
	@echo ""
	@echo "Testing:"
	@echo "  make test            Run all tests"
	@echo "  make lint            Run code quality checks"
	@echo "  make format          Format code"

infra-up:
	@echo "Starting infrastructure services..."
	docker compose up -d mariadb mongodb redis zookeeper kafka
	@echo "Waiting for infrastructure to be healthy..."
	@timeout=120; interval=5; elapsed=0; \
	while [ $$elapsed -lt $$timeout ]; do \
		healthy=true; \
		for svc in mariadb redis; do \
			status=$$(docker compose ps --format json $$svc 2>/dev/null | grep -o '"health_status":"[^"]*"' | cut -d'"' -f4); \
			if [ "$$status" != "healthy" ]; then healthy=false; fi; \
		done; \
		if [ "$$healthy" = true ]; then echo "Infrastructure ready!"; exit 0; fi; \
		sleep $$interval; elapsed=$$((elapsed + interval)); \
	done; \
	echo "Warning: Infrastructure may not be fully healthy after $$timeout seconds"

infra-down:
	docker compose down mariadb mongodb redis zookeeper kafka

infra-logs:
	docker compose logs -f mariadb mongodb redis zookeeper kafka

up:
	docker compose up -d --build

down:
	docker compose down

down-v:
	docker compose down -v

rebuild:
	docker compose down && docker compose up -d --build

build-all:
	docker compose build

build:
ifndef SERVICE
	$(error SERVICE is required. Usage: make build SERVICE=auth-service)
endif
	docker compose build $(SERVICE)

logs:
ifdef SERVICE
	docker compose logs -f $(SERVICE)
else
	docker compose logs -f
endif

ps:
	docker compose ps

hybrid-backend: infra-up
	@echo "Starting backend services in Docker..."
	docker compose up -d auth-service api-gateway payment-service order-service notification-service simulator-service analytics-service audit-service
	@echo "Backend services started. Run frontend locally with: make frontend-dev"

hybrid-frontend: infra-up
	@echo "Infrastructure ready. Start backend services locally, then run:"
	@echo "  cd web/frontend && npm run dev"

frontend-dev:
	cd web/frontend && npm run dev

frontend-build:
	cd web/frontend && npm run build

db-reset:
	docker compose down -v mariadb mongodb
	docker compose up -d mariadb mongodb
	@echo "Database volumes reset. Waiting for services to initialize..."
	sleep 10

test:
	mvn test -q
	cd web/frontend && npm test -- --run

lint:
	cd web/frontend && npm run lint

format:
	cd web/frontend && npm run format
