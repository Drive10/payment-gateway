# Payment Gateway - Makefile
# Works on Linux, macOS, and Windows (with make installed)
#
# Usage:
#   make help          Show help
#   make dev-infra     Start infrastructure for development
#   make dev-build     Build all services
#   make dev-run service=auth    Run a specific service
#   make docker-up     Start all services in Docker
#   make docker-down   Stop all services
#   make docker-status Show service status
#   make test-all      Run E2E tests
#   make hybrid scenario=network-partition   Run hybrid scenario
#   make clean         Clean up Docker resources

.PHONY: help dev-infra dev-stop dev-build dev-run dev-build-all dev-run \
        docker-up docker-up-obs docker-down docker-status docker-logs \
        test-health test-auth test-orders test-payments test-all \
        hybrid clean setup bootstrap doctor smoke verify \
        dev-seed dev-tail dev-test dev-health

# Detect OS
UNAME_S := $(shell uname -s 2>/dev/null || echo Windows)
ifeq ($(UNAME_S),Linux)
    DETECTED_OS := linux
    SH := bash
    SCRIPT_EXT := .sh
else ifeq ($(UNAME_S),Darwin)
    DETECTED_OS := mac
    SH := bash
    SCRIPT_EXT := .sh
else
    DETECTED_OS := windows
    SH := powershell -ExecutionPolicy Bypass -File
    SCRIPT_EXT := .ps1
endif

# Colors
CYAN := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
NC := \033[0m

# ============================================================================
# Help
# ============================================================================

help: ## Show this help
	@echo ""
	@echo "$(YELLOW)Payment Gateway - Available Commands$(NC)"
	@echo ""
	@echo "$(CYAN)Hybrid Dev (Recommended)$(NC)"
	@echo "  make dev-start              Start infra + gateway + frontends in Docker"
	@echo "  make dev-stop               Stop hybrid dev environment"
	@echo "  make dev-restart            Restart hybrid dev environment"
	@echo "  make dev-status             Show Docker + local service status"
	@echo "  make dev-run SERVICE=<name> Run a service locally with hot-reload"
	@echo "  make dev-build SERVICE=<name> Build a specific service"
	@echo "  make dev-logs SERVICE=<name> Follow Docker service logs"
	@echo "  make dev-db DB=<name>       Open psql to a database"
	@echo "  make dev-redis              Open redis-cli"
	@echo "  make dev-kafka-topics       List Kafka topics"
	@echo ""
	@echo "$(CYAN)Dev Tools$(NC)"
	@echo "  make dev-seed               Seed demo data (users, orders, payments)"
	@echo "  make dev-tail SERVICES='x y' Multi-service color log viewer"
	@echo "  make dev-test CMD=<cmd>     API test shortcuts (login, order, e2e)"
	@echo "  make dev-health             Check all service health"
	@echo ""
	@echo "$(CYAN)Full Docker Mode$(NC)"
	@echo "  make docker-up              Start ALL services in Docker"
	@echo "  make docker-up-obs          Start with observability stack"
	@echo "  make docker-down            Stop all services"
	@echo "  make docker-status          Show service status"
	@echo "  make docker-logs service=<name> Show logs for a service"
	@echo ""
	@echo "$(CYAN)Testing$(NC)"
	@echo "  make test-health            Test health endpoints"
	@echo "  make test-auth              Test authentication"
	@echo "  make test-orders            Test order endpoints"
	@echo "  make test-payments          Test payment endpoints"
	@echo "  make test-all               Run all E2E tests"
	@echo ""
	@echo "$(CYAN)Utilities$(NC)"
	@echo "  make clean                  Clean up Docker resources"
	@echo "  make setup                  Make scripts executable (Linux/Mac)"
	@echo ""
	@echo "$(YELLOW)Detected OS: $(DETECTED_OS)$(NC)"

# ============================================================================
# Legacy Commands (compatibility)
# ============================================================================

bootstrap:
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 bootstrap
else
	@echo "$(YELLOW)Bootstrap not implemented for $(DETECTED_OS)$(NC)"
endif

doctor:
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 doctor
else
	@echo "$(YELLOW)Doctor not implemented for $(DETECTED_OS)$(NC)"
endif

smoke:
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 smoke
else
	@echo "$(YELLOW)Smoke test not implemented for $(DETECTED_OS)$(NC)"
endif

verify:
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 verify
else
	@echo "$(YELLOW)Verify not implemented for $(DETECTED_OS)$(NC)"
endif

# ============================================================================
# Hybrid Development Mode (Infra in Docker, Services on localhost)
# ============================================================================

dev-start: ## Start infra + gateway + frontends in Docker (services run locally)
	@echo "$(CYAN)Starting hybrid dev environment...$(NC)"
	docker compose -f docker-compose.dev.yml --env-file .env.example up -d
	@echo "$(GREEN)Infra ready. Run services with: make dev-run SERVICE=payment-service$(NC)"

dev-stop: ## Stop hybrid dev environment
	@echo "$(CYAN)Stopping hybrid dev environment...$(NC)"
	docker compose -f docker-compose.dev.yml --env-file .env.example down
	@echo "$(GREEN)Stopped.$(NC)"

dev-restart: dev-stop dev-start ## Restart hybrid dev environment

dev-status: ## Show hybrid dev status
	@echo "$(CYAN)Docker services:$(NC)"
	@docker compose -f docker-compose.dev.yml --env-file .env.example ps 2>/dev/null || echo "  Not running"
	@echo ""
	@echo "$(CYAN)Local services (Maven):$(NC)"
		port=$$(echo $$svc | sed 's/-service//; s/auth/8081/; s/order/8082/; s/payment/8083/; s/notification/8084/; s/webhook/8085/; s/simulator/8086/; s/settlement/8087/; s/risk/8088/; s/analytics/8089/; s/merchant/8090/; s/dispute/8091/'); \
		if curl -s --connect-timeout 1 http://localhost:$$port/actuator/health >/dev/null 2>&1; then \
			echo "  $(GREEN)●$(NC) $$svc (port $$port)"; \
		else \
			echo "  $(RED)○$(NC) $$svc (port $$port)"; \
		fi; \
	done

dev-run: ## Run a service locally (usage: make dev-run SERVICE=payment-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: Specify service (make dev-run SERVICE=payment-service)$(NC)"; \
		echo "$(YELLOW)Available: auth-service, order-service, payment-service, notification-service,$(NC)"; \
		exit 1; \
	fi
	@echo "$(CYAN)Starting $(SERVICE) locally...$(NC)"
	@cd services/$(SERVICE) && \
		SPRING_PROFILES_ACTIVE=local \
		DB_HOST=localhost \
		DB_PORT=5433 \
		KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
		REDIS_HOST=localhost \
		REDIS_PORT=6379 \
		VAULT_ADDR=http://localhost:8200 \
		VAULT_TOKEN=dev-root-token \
		VAULT_ENABLED=true \
		AUTH_SERVICE_URL=http://localhost:8081 \
		ORDER_SERVICE_URL=http://localhost:8082 \
		PAYMENT_SERVICE_URL=http://localhost:8083 \
		NOTIFICATION_SERVICE_URL=http://localhost:8084 \
		WEBHOOK_SERVICE_URL=http://localhost:8085 \
		SIMULATOR_SERVICE_URL=http://localhost:8086 \
		SETTLEMENT_SERVICE_URL=http://localhost:8087 \
		RISK_SERVICE_URL=http://localhost:8088 \
		ANALYTICS_SERVICE_URL=http://localhost:8089 \
		MERCHANT_SERVICE_URL=http://localhost:8090 \
		DISPUTE_SERVICE_URL=http://localhost:8091 \
		mvn spring-boot:run

dev-build: ## Build a specific service (usage: make dev-build SERVICE=payment-service)
	@if [ -z "$(SERVICE)" ]; then \
		echo "$(RED)Error: Specify service (make dev-build SERVICE=payment-service)$(NC)"; \
		exit 1; \
	fi
	@echo "$(CYAN)Building $(SERVICE)...$(NC)"
	mvn -B -q -DskipTests -pl services/$(SERVICE) -am package
	@echo "$(GREEN)$(SERVICE) built!$(NC)"

dev-logs: ## Follow logs for a Docker service (usage: make dev-logs SERVICE=api-gateway)
	docker compose -f docker-compose.dev.yml --env-file .env.example logs -f $(SERVICE)

dev-db: ## Open psql to a database (usage: make dev-db DB=paymentdb)
	docker compose -f docker-compose.dev.yml --env-file .env.example exec postgres psql -U payment -d $(DB)

dev-redis: ## Open redis-cli
	docker compose -f docker-compose.dev.yml --env-file .env.example exec redis redis-cli

dev-kafka-topics: ## List Kafka topics
	docker compose -f docker-compose.dev.yml --env-file .env.example exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

dev-seed: ## Seed demo data (users, orders, payments)
	@./dev/seed.sh

dev-tail: ## Multi-service log viewer (usage: make dev-tail SERVICES="payment-service auth-service")
	@./dev/tail.sh $(SERVICES)

dev-test: ## API test shortcuts (usage: make dev-test CMD=login)
	@./dev/test.sh $(CMD)

dev-health: ## Check all service health
	@./dev/test.sh health

# ============================================================================
# Docker Mode
# ============================================================================

docker-up: ## Start all services in Docker
	@echo "$(CYAN)Starting all services...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-docker.ps1 -Up
else
	./run-docker.sh --up
endif

docker-up-obs: ## Start all services with observability stack
	@echo "$(CYAN)Starting all services with observability...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-docker.ps1 -Up -WithObservability
else
	./run-docker.sh --up --observability
endif

docker-down: ## Stop all services
	@echo "$(CYAN)Stopping all services...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-docker.ps1 -Down
else
	./run-docker.sh --down
endif

docker-status: ## Show service status
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-docker.ps1 -Status
else
	./run-docker.sh --status
endif

docker-logs: ## Show logs (usage: make docker-logs service=auth)
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-docker.ps1 -Logs $(if $(service),-Service $(service),)
else
	./run-docker.sh --logs $(service)
endif

# ============================================================================
# E2E Testing
# ============================================================================

test-health: ## Test health endpoints
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File test-e2e.ps1 -Health
else
	./test-e2e.sh --health
endif

test-auth: ## Test authentication endpoints
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File test-e2e.ps1 -Auth
else
	./test-e2e.sh --auth
endif

test-orders: ## Test order endpoints
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File test-e2e.ps1 -Orders
else
	./test-e2e.sh --orders
endif

test-payments: ## Test payment endpoints
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File test-e2e.ps1 -Payments
else
	./test-e2e.sh --payments
endif

test-all: ## Run all E2E tests
	@echo "$(CYAN)Running all E2E tests...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File test-e2e.ps1 -RunAll
else
	./test-e2e.sh --all
endif

# Legacy alias
up: docker-up
up-full: docker-up
up-hybrid: docker-up-obs
down: docker-down
test: test-all

# ============================================================================
# Hybrid Scenarios
# ============================================================================

hybrid: ## Run hybrid scenario (usage: make hybrid scenario=network-partition)
	@if [ -z "$(scenario)" ]; then \
		echo "$(RED)Error: Please specify a scenario$(NC)"; \
		echo "Available: infra-only, mixed, network-partition, service-restart, db-failover, kafka-lag"; \
		exit 1; \
	fi
	@echo "$(CYAN)Running scenario: $(scenario)...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-hybrid.ps1 -Scenario $(scenario)
else
	./run-hybrid.sh $(scenario)
endif

# ============================================================================
# Utilities
# ============================================================================

clean: ## Clean up Docker resources
	@echo "$(CYAN)Cleaning up...$(NC)"
	docker compose --profile services --profile infra --profile observability down -v 2>/dev/null || true
	docker system prune -f 2>/dev/null || true
	@echo "$(GREEN)Cleanup complete!$(NC)"

setup: ## Make scripts executable (Linux/macOS only)
ifeq ($(DETECTED_OS),linux)
	chmod +x *.sh
	@echo "$(GREEN)Scripts are now executable$(NC)"
else ifeq ($(DETECTED_OS),mac)
	chmod +x *.sh
	@echo "$(GREEN)Scripts are now executable$(NC)"
else
	@echo "$(YELLOW)Not needed on Windows$(NC)"
endif

# Maven commands
build:
	mvn -B -q -DskipTests package
