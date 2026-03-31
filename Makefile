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

.PHONY: help dev-infra dev-stop dev-build dev-run \
        docker-up docker-up-obs docker-down docker-status docker-logs \
        test-health test-auth test-orders test-payments test-all \
        hybrid clean setup bootstrap doctor smoke verify

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
	@echo "$(CYAN)Development Mode$(NC)"
	@echo "  make dev-infra              Start infrastructure (Postgres, Redis, Kafka)"
	@echo "  make dev-stop               Stop infrastructure"
	@echo "  make dev-build              Build all services with Maven"
	@echo "  make dev-run service=<name> Run a specific service locally"
	@echo ""
	@echo "$(CYAN)Docker Mode$(NC)"
	@echo "  make docker-up              Start all services in Docker"
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
	@echo "$(CYAN)Hybrid Scenarios$(NC)"
	@echo "  make hybrid scenario=infra-only"
	@echo "  make hybrid scenario=mixed"
	@echo "  make hybrid scenario=network-partition"
	@echo "  make hybrid scenario=service-restart"
	@echo "  make hybrid scenario=db-failover"
	@echo "  make hybrid scenario=kafka-lag"
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
# Development Mode
# ============================================================================

dev-infra: ## Start infrastructure services for local development
	@echo "$(CYAN)Starting infrastructure...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-dev.ps1 -StartInfra
else
	./run-dev.sh --start-infra
endif

dev-stop: ## Stop infrastructure services
	@echo "$(CYAN)Stopping infrastructure...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-dev.ps1 -StopInfra
else
	./run-dev.sh --stop-infra
endif

dev-build: ## Build all services with Maven
	@echo "$(CYAN)Building all services...$(NC)"
	mvn clean package -DskipTests
	@echo "$(GREEN)Build complete!$(NC)"

dev-run: ## Run a specific service (usage: make dev-run service=auth)
	@if [ -z "$(service)" ]; then \
		echo "$(RED)Error: Please specify a service (e.g., make dev-run service=auth)$(NC)"; \
		exit 1; \
	fi
	@echo "$(CYAN)Starting $(service) service...$(NC)"
ifeq ($(DETECTED_OS),windows)
	powershell -ExecutionPolicy Bypass -File run-dev.ps1 -Service $(service)
else
	./run-dev.sh --service $(service)
endif

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

verify:
	mvn verify
