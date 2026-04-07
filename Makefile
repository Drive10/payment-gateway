.PHONY: help \
  infra-up infra-down infra-logs infra-status \
  up down down-v rebuild \
  build-all build build-no-cache build-pull \
  logs logs-follow logs-service \
  ps ps-services status \
  health health-service \
  diagnose \
  dev dev-clean dev-rebuild \
  test test-backend test-frontend test-integration \
  lint lint-backend lint-frontend \
  format format-backend format-frontend \
  frontend-dev frontend-build frontend-clean \
  db-reset db-migrate db-seed \
  monitoring-up monitoring-down \
  clean-volumes clean-images \
  docker-prune default-network

help:
	@echo "PayFlow - Development & Production Commands"
	@echo ""
	@echo "=========================================="
	@echo "  QUICK START"
	@echo "=========================================="
	@echo "  make dev          - Clean start for development"
	@echo "  make up           - Start all services"
	@echo "  make diagnose     - Run startup diagnostics"
	@echo ""
	@echo "=========================================="
	@echo "  INFRASTRUCTURE"
	@echo "=========================================="
	@echo "  make infra-up       Start infrastructure (MariaDB, MongoDB, Redis, Kafka)"
	@echo "  make infra-down     Stop infrastructure"
	@echo "  make infra-logs     View infrastructure logs"
	@echo "  make infra-status  Show infrastructure status"
	@echo ""
	@echo "=========================================="
	@echo "  FULL STACK"
	@echo "=========================================="
	@echo "  make up            Start all services"
	@echo "  make down          Stop all services"
	@echo "  make down-v        Stop + remove volumes"
	@echo "  make rebuild       Rebuild and restart"
	@echo "  make dev           Clean dev start (down-v + up)"
	@echo ""
	@echo "=========================================="
	@echo "  BUILD"
	@echo "=========================================="
	@echo "  make build-all      Build all Docker images"
	@echo "  make build SERVICE=x Build single service"
	@echo "  make build-no-cache Build without cache"
	@echo "  make build-pull     Pull latest base images"
	@echo ""
	@echo "=========================================="
	@echo "  LOGS & DEBUGGING"
	@echo "=========================================="
	@echo "  make logs           View all logs"
	@echo "  make logs SERVICE=x View specific service"
	@echo "  make ps             Show container status"
	@echo "  make status         Show detailed status"
	@echo "  make health         Show health status"
	@echo "  make diagnose       Run diagnostics"
	@echo ""
	@echo "=========================================="
	@echo "  DEVELOPMENT"
	@echo "=========================================="
	@echo "  make dev            Clean development start"
	@echo "  make dev-rebuild    Rebuild without down"
	@echo "  make frontend-dev   Run frontend dev server"
	@echo "  make frontend-build Build frontend"
	@echo ""
	@echo "=========================================="
	@echo "  TESTING"
	@echo "=========================================="
	@echo "  make test           Run all tests"
	@echo "  make test-backend   Run backend tests"
	@echo "  make test-frontend Run frontend tests"
	@echo "  make lint           Run linters"
	@echo "  make format         Format code"
	@echo ""
	@echo "=========================================="
	@echo "  DATABASE"
	@echo "=========================================="
	@echo "  make db-reset       Reset database volumes"
	@echo "  make db-migrate     Run migrations"
	@echo "  make db-seed        Seed database"
	@echo ""
	@echo "=========================================="
	@echo "  MONITORING"
	@echo "=========================================="
	@echo "  make monitoring-up  Start Prometheus + Grafana"
	@echo "  make monitoring-down Stop monitoring"
	@echo ""
	@echo "=========================================="
	@echo "  CLEANUP"
	@echo "=========================================="
	@echo "  make clean-volumes  Remove all volumes"
	@echo "  make clean-images   Remove unused images"
	@echo "  make docker-prune   Full Docker cleanup"

# ===========================================
# Infrastructure
# ===========================================

infra-up:
	@echo "Starting infrastructure services..."
	@docker compose up -d mariadb mongodb redis zookeeper kafka
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
	echo "Warning: Infrastructure may not be fully healthy"

infra-down:
	docker compose down mariadb mongodb redis zookeeper kafka

infra-logs:
	docker compose logs -f mariadb mongodb redis zookeeper kafka

infra-status:
	@docker compose ps mariadb mongodb redis zookeeper kafka

# ===========================================
# Full Stack
# ===========================================

up:
	docker compose up -d --build

down:
	docker compose down

down-v:
	docker compose down -v

rebuild:
	docker compose down && docker compose up -d --build

dev: down-v
	@echo "Starting clean development environment..."
	docker compose up -d --build

dev-clean:
	docker compose down -v
	docker system prune -f --volumes || true

dev-rebuild:
	docker compose up -d --build --force-recreate

# ===========================================
# Build
# ===========================================

build-all:
	docker compose build

build:
ifndef SERVICE
	$(error SERVICE is required. Usage: make build SERVICE=auth-service)
endif
	docker compose build $(SERVICE)

build-no-cache:
	docker compose build --no-cache

build-pull:
	docker compose pull

# ===========================================
# Logs & Debugging
# ===========================================

logs:
ifneq ($(filter-out $@,$(MAKECMDGOALS)),)
	docker compose logs -f $(filter-out $@,$(MAKECMDGOALS))
else
	docker compose logs -f
endif

logs-follow:
	docker compose logs -f --tail=100

logs-service:
ifndef SERVICE
	$(error SERVICE is required. Usage: make logs SERVICE=auth-service)
endif
	docker compose logs -f $(SERVICE)

ps:
	docker compose ps

status:
	docker compose ps -a

health:
	@echo "Checking service health..."
	@for svc in auth-service api-gateway payment-service order-service notification-service simulator-service analytics-service audit-service; do \
		status=$$(docker compose ps --format json $$svc 2>/dev/null | grep -o '"health_status":"[^"]*"' | cut -d'"' -f4); \
		if [ "$$status" = "healthy" ]; then \
			echo "✓ $$svc: healthy"; \
		elif [ "$$status" = "starting" ]; then \
			echo "◐ $$svc: starting"; \
		else \
			echo "✗ $$svc: $$status"; \
		fi; \
	done

health-service:
ifndef SERVICE
	$(error SERVICE is required. Usage: make health SERVICE=auth-service)
endif
	@docker compose exec $(SERVICE) wget --spider -q http://localhost:8080/actuator/health || \
	docker compose exec $(SERVICE) wget --spider -q http://localhost:8081/actuator/health || \
	echo "Service $(SERVICE) health check failed"

diagnose:
	@bash scripts/diagnose.sh

# ===========================================
# Frontend
# ===========================================

frontend-dev:
	@echo "Starting frontend dev server..."
	cd web/frontend && npm run dev

frontend-build:
	cd web/frontend && npm run build

frontend-clean:
	cd web/frontend && rm -rf dist node_modules/.vite
	cd web/frontend && npm install

# ===========================================
# Testing
# ===========================================

test: test-backend test-frontend

test-backend:
	@echo "Running backend tests..."
	cd $(shell pwd) && mvn test -q

test-frontend:
	@echo "Running frontend tests..."
	cd web/frontend && npm test -- --run

test-integration:
	@echo "Running integration tests..."
	cd $(shell pwd) && mvn verify -Pintegration

# ===========================================
# Linting & Formatting
# ===========================================

lint: lint-backend lint-frontend

lint-backend:
	@echo "Linting backend code..."
	cd $(shell pwd) && mvn spotless:check || true

lint-frontend:
	cd web/frontend && npm run lint

format: format-backend format-frontend

format-backend:
	cd $(shell pwd) && mvn spotless:apply

format-frontend:
	cd web/frontend && npm run format

# ===========================================
# Database
# ===========================================

db-reset:
	@echo "Resetting database volumes..."
	docker compose down -v mariadb mongodb
	docker compose up -d mariadb mongodb
	@echo "Waiting for databases to initialize..."
	sleep 15
	@echo "Database reset complete"

db-migrate:
	@echo "Running database migrations..."
	docker compose exec payment-service java -jar app.jar spring:jpa:hibernate:ddl-auto:update

db-seed:
	@echo "Seeding database..."
	docker exec -i mariadb mysql -uroot -prootpassword payment_gateway < scripts/init-db.sql 2>/dev/null || \
	echo "Note: Run manually after DB is ready: docker exec -i mariadb mysql -uroot -prootpassword payment_gateway < scripts/init-db.sql"

# ===========================================
# Monitoring
# ===========================================

monitoring-up:
	@echo "Starting monitoring stack..."
	docker compose -f docker-compose.monitoring.yml up -d
	@echo "Grafana: http://localhost:3001 (admin/admin)"
	@echo "Prometheus: http://localhost:9090"

monitoring-down:
	docker compose -f docker-compose.monitoring.yml down

# ===========================================
# Cleanup
# ===========================================

clean-volumes:
	docker compose down -v
	@echo "All volumes removed"

clean-images:
	@echo "Removing unused images..."
	docker image prune -f

docker-prune:
	docker system prune -af --volumes
	@echo "Docker cleanup complete"

default-network:
	docker network create payflow-network 2>/dev/null || true