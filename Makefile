.PHONY: help \
  infra-up infra-down infra-logs infra-status \
  logs ps status health diagnose \
  dev dev-stop \
  test lint format \
  frontend-dev frontend-build \
  clean docker-prune

help:
	@echo "PayFlow - Development Commands"
	@echo ""
	@echo "  INFRASTRUCTURE"
	@echo "    make infra-up      - Start Docker infra (postgres, redis, kafka, etc)"
	@echo "    make infra-down    - Stop Docker infra"
	@echo "    make infra-logs    - View infra logs"
	@echo "    make infra-status  - Show infra status"
	@echo ""
	@echo "  DEVELOPMENT"
	@echo "    make dev           - Start all services locally"
	@echo "    make dev-stop      - Stop all local services"
	@echo "    make diagnose      - Health check all services"
	@echo ""
	@echo "  LOGS & STATUS"
	@echo "    make logs          - View Docker logs"
	@echo "    make ps            - Show Docker containers"
	@echo "    make status        - Show detailed status"
	@echo "    make health        - Check service health"
	@echo ""
	@echo "  CODE"
	@echo "    make test          - Run all tests"
	@echo "    make lint          - Run linters"
	@echo "    make format        - Format code"
	@echo ""
	@echo "  FRONTEND"
	@echo "    make frontend-dev  - Start frontend dev server"
	@echo "    make frontend-build - Build frontend"
	@echo ""
	@echo "  CLEANUP"
	@echo "    make clean         - Stop infra and remove volumes"
	@echo "    make docker-prune  - Full Docker cleanup"

# ===========================================
# Infrastructure
# ===========================================

infra-up:
	@echo "Starting infrastructure..."
	docker compose up -d postgres mongodb redis zookeeper kafka zipkin
	@echo "Waiting for infrastructure..."
	@timeout=120; interval=5; elapsed=0; \
	while [ $$elapsed -lt $$timeout ]; do \
		healthy=true; \
		for svc in postgres redis; do \
			status=$$(docker compose ps --format json $$svc 2>/dev/null | grep -o '"health_status":"[^"]*"' | cut -d'"' -f4); \
			if [ "$$status" != "healthy" ]; then healthy=false; fi; \
		done; \
		if [ "$$healthy" = true ]; then echo "Infrastructure ready!"; exit 0; fi; \
		sleep $$interval; elapsed=$$((elapsed + interval)); \
	done; \
	echo "Warning: Some services may still be starting"

infra-down:
	docker compose down

infra-logs:
	docker compose logs -f postgres mongodb redis kafka

infra-status:
	@docker compose ps postgres mongodb redis kafka

# ===========================================
# Development
# ===========================================

dev:
	./run-local.sh

dev-stop:
	@pkill -f "spring-boot:run" || true
	@pkill -f "vite" || true
	@echo "Local services stopped"

# ===========================================
# Logs & Status
# ===========================================

logs:
	docker compose logs -f

ps:
	docker compose ps

status:
	docker compose ps -a

health:
	@echo "Checking service health..."
	@for port in 8080 8081 8082 8083 8084 8086 8089 8090; do \
		if curl -sf "http://localhost:$$port/actuator/health" > /dev/null 2>&1; then \
			echo "✓ port $$port: healthy"; \
		else \
			echo "○ port $$port: not running"; \
		fi; \
	done

diagnose:
	@bash scripts/diagnose.sh

# ===========================================
# Testing
# ===========================================

test:
	@echo "Running tests..."
	mvn test -q

lint:
	@echo "Running linters..."
	mvn spotless:check || true
	cd web/frontend && npm run lint || true

format:
	@echo "Formatting code..."
	mvn spotless:apply
	cd web/frontend && npm run format || true

# ===========================================
# Frontend
# ===========================================

frontend-dev:
	@echo "Starting frontend..."
	cd web/frontend && npm run dev

frontend-build:
	cd web/frontend && npm run build

# ===========================================
# Cleanup
# ===========================================

clean:
	docker compose down -v
	@echo "Docker volumes removed"

docker-prune:
	docker system prune -af --volumes
	@echo "Docker cleanup complete"
