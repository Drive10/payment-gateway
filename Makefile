.PHONY: help \
  docker-up docker-down \
  infra-up infra-down infra-logs \
  dev dev-stop \
  logs ps health diagnose \
  test lint format \
  k8s-validate k8s-deploy-staging k8s-deploy-prod \
  helm-install-staging helm-install-prod \
  hooks-install clean

help:
	@echo "PayFlow - Development Commands"
	@echo ""
	@echo "  DOCKER MODE (Full stack in Docker)"
	@echo "    make docker-up      - Start infra + all services in Docker"
	@echo "    make docker-down    - Stop all Docker services"
	@echo ""
	@echo "  LOCAL MODE (Backend local, infra in Docker)"
	@echo "    make infra-up      - Start Docker infra only"
	@echo "    make infra-down    - Stop Docker infra"
	@echo "    make dev           - Start backend services locally (IDE)"
	@echo "    make dev-stop      - Stop local backend services"
	@echo ""
	@echo "  UTILS"
	@echo "    make logs          - View Docker logs"
	@echo "    make ps            - Show Docker containers"
	@echo "    make health        - Check service health"
	@echo "    make diagnose      - Full diagnostics"
	@echo "    make test          - Run tests"
	@echo "    make lint          - Run linters"
	@echo "    make format        - Format code"
	@echo ""
	@echo "  KUBERNETES"
	@echo "    make k8s-validate    - Validate K8s configs"
	@echo "    make k8s-deploy-staging - Deploy to staging"
	@echo "    make k8s-deploy-prod  - Deploy to production"
	@echo ""
	@echo "  HELM"
	@echo "    make helm-install-staging - Install Helm chart staging"
	@echo "    make helm-install-prod  - Install Helm chart production"
	@echo ""
	@echo "  PRE-COMMIT"
	@echo "    make hooks-install    - Install pre-commit hooks"
	@echo ""
	@echo "  CLEANUP"
	@echo "    make clean         - Stop and remove volumes"

# ===========================================
# Docker Mode (Full stack)
# ===========================================

docker-up:
	@echo "Starting full stack in Docker..."
	docker compose --profile services up -d --wait
	@echo "All services ready!"
	@echo "  API Gateway: http://localhost:8080"
	@echo "  Frontend:   http://localhost:5173"

docker-down:
	docker compose --profile services down

# ===========================================
# Local Mode (Backend local, infra Docker)
# ===========================================

infra-up:
	@echo "Starting infrastructure..."
	docker compose --profile infra up -d --wait
	@echo "Infrastructure ready!"

infra-down:
	docker compose down

infra-logs:
	docker compose logs -f

dev:
	./run-local.sh

dev-stop:
	@pkill -f "spring-boot:run" || true
	@pkill -f "vite" || true
	@echo "Local services stopped"

# ===========================================
# Utils
# ===========================================

logs:
	docker compose logs -f

ps:
	docker compose ps

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
	cd frontend/payment-page && npm run lint || true

format:
	@echo "Formatting code..."
	mvn spotless:apply
	cd frontend/payment-page && npm run format || true

# ===========================================
# Kubernetes (Kustomize)
# ===========================================

k8s-validate:
	kubectl kustomize config/k8s/overlays/staging --dry-run=server
	kubectl kustomize config/k8s/overlays/production --dry-run=server

k8s-deploy-staging:
	kubectl apply -k config/k8s/overlays/staging
	kubectl rollout status deployment -n payflow-staging

k8s-deploy-prod:
	kubectl apply -k config/k8s/overlays/production
	kubectl rollout status deployment -n payflow-prod

# ===========================================
# Helm
# ===========================================

helm-install-staging:
	helm upgrade --install payflow-staging config/helm/payflow \
		--namespace payflow-staging \
		--create-namespace \
		--set image.tag=staging \
		-f config/helm/payflow/values-staging.yaml

helm-install-prod:
	helm upgrade --install payflow-prod config/helm/payflow \
		--namespace payflow-prod \
		--create-namespace \
		--set image.tag=latest \
		-f config/helm/payflow/values-production.yaml

# ===========================================
# Pre-commit
# ===========================================

hooks-install:
	@echo "Installing pre-commit hooks..."
	pre-commit install

# ===========================================
# Cleanup
# ===========================================

clean:
	docker compose down -v
	@echo "Docker stopped and volumes removed"
