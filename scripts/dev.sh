#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/services/frontend"
COMMAND="${1:-help}"

step() {
  echo ""
  echo "=========================================="
  echo " $1"
  echo "=========================================="
}

has_cmd() {
  command -v "$1" >/dev/null 2>&1
}

require_cmd() {
  local name="$1"
  local hint="$2"
  if ! has_cmd "$name"; then
    echo "$name is not available. $hint" >&2
    exit 1
  fi
}

show_help() {
  cat <<'EOF'
Fintech Payment Gateway - Developer Scripts

Usage: ./scripts/dev.sh <command>

Infrastructure:
  infra           Start PostgreSQL, Kafka, Redis (Docker)
  infra-down      Stop infrastructure containers
  infra-logs      Show infrastructure logs

Modes:
  docker          Full Docker stack (recommended for demo)
  hybrid          Docker infra + local payment-service
  local           Full local development (no Docker)

Management:
  build           Build all services
  build-service   Build specific service (payment-service/api-gateway)
  down            Stop all Docker containers
  logs            Show logs for running services
  status          Show status of all services

Utilities:
  doctor          Check prerequisites (Java, Maven, Docker)
  swagger         Open Swagger UI in browser
  clean           Clean build artifacts

Quick Start:
  1. ./scripts/dev.sh doctor
  2. ./scripts/dev.sh infra
  3. ./scripts/dev.sh docker

EOF
}

bootstrap() {
  step "Preparing local workspace"
  if [[ ! -f "$REPO_ROOT/.env" && -f "$REPO_ROOT/.env.example" ]]; then
    cp "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
    echo "Created .env from .env.example"
  elif [[ -f "$REPO_ROOT/.env" ]]; then
    echo ".env already exists - skipping"
  fi

  if has_cmd node && has_cmd npm; then
    step "Installing frontend dependencies"
    cd "$FRONTEND_DIR" && npm ci
  fi

  step "Building backend"
  cd "$REPO_ROOT" && ./mvnw clean install -DskipTests -q
  
  echo ""
  echo "Setup complete! Next steps:"
  echo "  1. ./scripts/dev.sh doctor"
  echo "  2. ./scripts/dev.sh infra"
  echo "  3. ./scripts/dev.sh docker"
}

doctor() {
  step "Checking Prerequisites"
  echo ""
  
  local failed=0
  
  if has_cmd java; then
    printf "  %-20s ✓ ($(java -version 2>&1 | head -1))\n" "Java"
  else
    printf "  %-20s ✗ MISSING\n" "Java"
    failed=1
  fi
  
  if has_cmd mvn; then
    printf "  %-20s ✓ ($(mvn -version 2>&1 | head -1 | cut -d' ' -f3))\n" "Maven"
  else
    printf "  %-20s ✗ MISSING\n" "Maven"
    failed=1
  fi
  
  if has_cmd docker && docker info >/dev/null 2>&1; then
    printf "  %-20s ✓ Running\n" "Docker"
  else
    printf "  %-20s ⚠ Not running\n" "Docker"
  fi
  
  if has_cmd node; then
    printf "  %-20s ✓ (v%s)\n" "Node.js" "$(node -v)"
  else
    printf "  %-20s (optional)\n" "Node.js"
  fi
  
  echo ""
  if [[ ! -f "$REPO_ROOT/.env" ]]; then
    echo "  .env              ⚠ Create with: cp .env.example .env"
  else
    echo "  .env              ✓"
  fi
  
  echo ""
  if [[ "$failed" -ne 0 ]]; then
    echo "Please install missing required tools."
    exit 1
  fi
}

infra() {
  require_cmd docker "Install Docker Desktop"
  step "Starting Infrastructure (PostgreSQL, Kafka, Redis)"
  cd "$REPO_ROOT"
  docker compose --profile infra up -d
  echo ""
  echo "Waiting for services to be ready..."
  sleep 10
  docker compose ps
  echo ""
  echo "Infrastructure ready! Connect with:"
  echo "  PostgreSQL: localhost:5433"
  echo "  Kafka:      localhost:9092"
  echo "  Redis:      localhost:6379"
}

infra-down() {
  step "Stopping Infrastructure"
  cd "$REPO_ROOT"
  docker compose --profile infra down
}

docker() {
  require_cmd docker "Install Docker Desktop"
  step "Starting Full Docker Stack"
  cd "$REPO_ROOT"
  docker compose --profile services up -d --build
  echo ""
  echo "Waiting for services to start..."
  sleep 15
  docker compose ps
  echo ""
  echo "Services ready!"
  echo "  Frontend:     http://localhost:3000"
  echo "  Gateway:      http://localhost:8080"
  echo "  Swagger:      http://localhost:8080/swagger-ui.html"
  echo "  Prometheus:   http://localhost:9090"
  echo "  Grafana:      http://localhost:3001"
}

hybrid() {
  require_cmd docker "Install Docker Desktop"
  step "Starting Hybrid Mode (Docker Infra + Local Services)"
  cd "$REPO_ROOT"
  docker compose --profile infra up -d
  echo ""
  echo "Docker infra started. Now run in another terminal:"
  echo "  ./scripts/dev.sh payment-local"
  echo ""
  docker compose ps
}

payment-local() {
  step "Starting Local payment-service"
  cd "$REPO_ROOT"
  echo "Make sure Docker infrastructure is running: ./scripts/dev.sh infra"
  echo ""
  SPRING_PROFILES_ACTIVE=local ./mvnw -pl services/payment-service -am spring-boot:run
}

local_mode() {
  step "Starting Full Local Development"
  cd "$REPO_ROOT"
  echo "Starting PostgreSQL, Kafka, Redis via Docker..."
  docker compose --profile infra up -d
  sleep 10
  
  echo ""
  echo "Starting payment-service..."
  SPRING_PROFILES_ACTIVE=local ./mvnw -pl services/payment-service -am spring-boot:run &
  PAYMENT_PID=$!
  
  echo ""
  echo "payment-service starting on port 8083 (PID: $PAYMENT_PID)"
  echo "Press Ctrl+C to stop all services"
  
  wait $PAYMENT_PID
}

build() {
  step "Building All Services"
  cd "$REPO_ROOT"
  ./mvnw clean package -DskipTests
  echo ""
  echo "Build complete! JAR files:"
  ls -la services/*/target/*.jar 2>/dev/null || echo "No JAR files found"
}

build-service() {
  local service="${2:-payment-service}"
  step "Building $service"
  cd "$REPO_ROOT"
  ./mvnw clean package -DskipTests -pl services/$service -am
}

down() {
  step "Stopping All Containers"
  cd "$REPO_ROOT"
  docker compose down
  echo "All containers stopped."
}

logs() {
  local service="${2:-}"
  cd "$REPO_ROOT"
  if [[ -z "$service" ]]; then
    docker compose logs -f
  else
    docker compose logs -f "$service"
  fi
}

status() {
  step "Service Status"
  cd "$REPO_ROOT"
  echo ""
  docker compose ps
  echo ""
  echo "Local ports:"
  if has_cmd lsof; then
    lsof -i :8080 -i :8083 -i :3000 -i :5433 -i :9092 -i :6379 2>/dev/null | grep LISTEN || echo "  No local services detected"
  fi
}

swagger() {
  step "Opening Swagger UI"
  if has_cmd open; then
    open "http://localhost:8080/swagger-ui.html"
  else
    echo "Open in browser: http://localhost:8080/swagger-ui.html"
  fi
}

clean() {
  step "Cleaning Build Artifacts"
  cd "$REPO_ROOT"
  ./mvnw clean
  rm -rf services/*/target
  echo "Clean complete."
}

case "$COMMAND" in
  bootstrap) bootstrap ;;
  doctor) doctor ;;
  infra) infra ;;
  infra-down) infra-down ;;
  docker) docker ;;
  hybrid) hybrid ;;
  local) local_mode ;;
  build) build ;;
  build-service) build-service ;;
  down) down ;;
  logs) logs ;;
  status) status ;;
  swagger) swagger ;;
  clean) clean ;;
  payment-local) payment-local ;;
  help) show_help ;;
  *)
    show_help
    exit 1
    ;;
esac
