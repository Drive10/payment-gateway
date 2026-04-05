#!/usr/bin/env bash
set -euo pipefail

# PayFlow Smart Dev Environment Manager
# Usage: ./scripts/dev.sh [command] [options]
# Examples:
#   ./scripts/dev.sh up              # Start everything (infra + services + web)
#   ./scripts/dev.sh up --local      # Infra in Docker, services locally (hot reload)
#   ./scripts/dev.sh up --docker     # Everything in Docker
#   ./scripts/dev.sh status          # Show status of all services
#   ./scripts/dev.sh logs payment    # Tail payment-service logs
#   ./scripts/dev.sh down            # Stop everything

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose.dev.yml"
ENV_FILE="$REPO_ROOT/.env"
LOG_DIR="/tmp/payflow"
PID_DIR="$LOG_DIR/pids"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Service definitions: name|port|docker_port_offset
declare -a BACKEND_SERVICES=(
  "auth-service|8081"
  "order-service|8082"
  "payment-service|8083"
  "notification-service|8084"
  "analytics-service|8085"
  "simulator-service|8086"
)

INFRA_SERVICES=("postgres" "redis" "kafka" "vault")
DOCKER_SERVICES=()
ALL_BACKEND_SERVICES=()

# Helpers
log_info()    { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok()      { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_err()     { echo -e "${RED}[ERR]${NC} $1"; }
log_header()  { echo -e "\n${CYAN}=== $1 ===${NC}"; }

mkdir -p "$LOG_DIR" "$PID_DIR"

# ── Prerequisites ──────────────────────────────────────────────
require_docker() {
  if ! docker info >/dev/null 2>&1; then
    log_err "Docker is not running. Start Docker Desktop first."
    exit 1
  fi
}

require_maven() {
  if ! command -v mvn >/dev/null 2>&1; then
    log_err "Maven is not installed."
    exit 1
  fi
}

require_node() {
  if ! command -v node >/dev/null 2>&1; then
    log_err "Node.js is not installed."
    exit 1
  fi
}

# ── Infrastructure ─────────────────────────────────────────────
infra_start() {
  require_docker
  log_info "Starting infrastructure (Postgres, Redis, Kafka)..."
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d "${INFRA_SERVICES[@]}"
  log_info "Waiting for infrastructure health checks..."
  for svc in "${INFRA_SERVICES[@]}"; do
    local retries=0
    while [ $retries -lt 30 ]; do
      if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps "$svc" 2>/dev/null | grep -qi "healthy\|Up"; then
        log_ok "$svc is running"
        break
      fi
      retries=$((retries + 1))
      sleep 2
    done
    if [ $retries -eq 30 ]; then
      log_err "$svc failed to start"
    fi
  done
}

infra_stop() {
  log_info "Stopping infrastructure..."
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down 2>/dev/null || true
  log_ok "Infrastructure stopped"
}

# ── Docker Services ────────────────────────────────────────────
docker_start() {
  require_docker
  if [ ${#DOCKER_SERVICES[@]} -eq 0 ]; then
    log_info "No Docker services to start"
    return 0
  fi
  log_info "Starting Docker services..."
  if [ ${#DOCKER_SERVICES[@]} -gt 0 ]; then
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d "${DOCKER_SERVICES[@]}"
  fi
  log_ok "Docker services started"
}

docker_stop() {
  log_info "Stopping Docker services..."
  if [ ${#DOCKER_SERVICES[@]} -gt 0 ]; then
    docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" stop "${DOCKER_SERVICES[@]}" 2>/dev/null || true
  fi
  log_ok "Docker services stopped"
}

# ── Local Backend Services ─────────────────────────────────────
setup_env() {
  export DB_HOST="${DB_HOST:-localhost}"
  export DB_PORT="${DB_PORT:-5433}"
  export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
  export REDIS_HOST="${REDIS_HOST:-localhost}"
  export REDIS_PORT="${REDIS_PORT:-6379}"
  export REDIS_PASSWORD="${REDIS_PASSWORD:-devpassword}"
  export VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
  export VAULT_TOKEN="${VAULT_TOKEN:-dev-root-token}"
  export VAULT_ENABLED="${VAULT_ENABLED:-false}"
  export GATEWAY_INTERNAL_SECRET="${GATEWAY_INTERNAL_SECRET:-dev-gateway-secret}"
  export JWT_SECRET="${JWT_SECRET:-dev-jwt-secret-key-must-be-at-least-256-bits-long-for-hs512}"
}

get_db_config() {
  local service=$1
  case "$service" in
    auth-service)
      export DB_NAME="authdb"
      export DB_USERNAME="auth"
      export DB_PASSWORD="devpassword"
      ;;
    order-service)
      export DB_NAME="orderdb"
      export DB_USERNAME="payment"
      export DB_PASSWORD="devpassword"
      ;;
    payment-service)
      export DB_NAME="paymentdb"
      export DB_USERNAME="paymentuser"
      export DB_PASSWORD="devpassword"
      ;;
    notification-service)
      export DB_NAME="notificationdb"
      export DB_USERNAME="notification"
      export DB_PASSWORD="devpassword"
      ;;
    analytics-service)
      export DB_NAME="analyticsdb"
      export DB_USERNAME="analytics"
      export DB_PASSWORD="devpassword"
      ;;
    simulator-service)
      export DB_NAME="simulatordb"
      export DB_USERNAME="simulator"
      export DB_PASSWORD="devpassword"
      ;;
  esac
}

start_local_service() {
  local service=$1
  local port=$2
  local pid_file="$PID_DIR/$service.pid"
  local log_file="$LOG_DIR/$service.log"

  # Kill existing if running
  if [ -f "$pid_file" ]; then
    local old_pid=$(cat "$pid_file")
    if kill -0 "$old_pid" 2>/dev/null; then
      kill "$old_pid" 2>/dev/null || true
      sleep 1
    fi
    rm -f "$pid_file"
  fi

  # Kill anything on the port
  if command -v lsof >/dev/null 2>&1; then
    local existing_pid=$(lsof -ti "tcp:$port" -sTCP:LISTEN 2>/dev/null | head -1 || true)
    if [ -n "$existing_pid" ]; then
      kill "$existing_pid" 2>/dev/null || true
      sleep 1
    fi
  fi

  log_info "Starting $service on port $port..."
  nohup mvn -pl "services/$service" spring-boot:run -q -DskipTests \
    -Dspring-boot.run.jvmArguments="-DDB_HOST=$DB_HOST -DDB_PORT=$DB_PORT -DDB_NAME=${DB_NAME:-} -DDB_USERNAME=${DB_USERNAME:-} -DDB_PASSWORD=${DB_PASSWORD:-} -DKAFKA_BOOTSTRAP_SERVERS=$KAFKA_BOOTSTRAP_SERVERS -DREDIS_HOST=$REDIS_HOST -DREDIS_PORT=$REDIS_PORT -DREDIS_PASSWORD=${REDIS_PASSWORD:-} -DVAULT_ADDR=$VAULT_ADDR -DVAULT_TOKEN=$VAULT_TOKEN -DVAULT_ENABLED=$VAULT_ENABLED -DJWT_SECRET=${JWT_SECRET:-dev-jwt-secret}" \
    > "$log_file" 2>&1 &

  echo $! > "$pid_file"
  log_ok "$service started (PID: $!, port: $port)"
}

stop_local_service() {
  local service=$1
  local pid_file="$PID_DIR/$service.pid"

  if [ -f "$pid_file" ]; then
    local pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      log_ok "$service stopped (PID: $pid)"
    fi
    rm -f "$pid_file"
  else
    pkill -f "$service" 2>/dev/null && log_ok "$service stopped" || log_warn "$service was not running"
  fi
}

services_start_local() {
  require_maven
  setup_env
  log_info "Starting all backend services locally (hot reload enabled)..."
  for entry in "${BACKEND_SERVICES[@]}"; do
    IFS='|' read -r service port <<< "$entry"
    get_db_config "$service"
    start_local_service "$service" "$port"
    sleep 2
  done
  log_info "All services starting... wait 60s for full startup"
}

services_stop_local() {
  log_info "Stopping all backend services..."
  for entry in "${BACKEND_SERVICES[@]}"; do
    IFS='|' read -r service port <<< "$entry"
    stop_local_service "$service"
  done
  log_ok "All backend services stopped"
}

# ── Web Apps ───────────────────────────────────────────────────
web_start() {
  require_node
  log_info "Starting web applications (Vite dev servers)..."

  # Dashboard
  cd "$REPO_ROOT/web/dashboard"
  nohup npm run dev > "$LOG_DIR/dashboard.log" 2>&1 &
  echo $! > "$PID_DIR/dashboard.pid"
  log_ok "Dashboard started (PID: $!, http://localhost:5173)"

  # Frontend
  cd "$REPO_ROOT/web/frontend"
  nohup npm run dev > "$LOG_DIR/frontend.log" 2>&1 &
  echo $! > "$PID_DIR/frontend.pid"
  log_ok "Frontend started (PID: $!, http://localhost:5174)"

  cd "$REPO_ROOT"
}

web_stop() {
  log_info "Stopping web applications..."
  for app in dashboard frontend; do
    local pid_file="$PID_DIR/$app.pid"
    if [ -f "$pid_file" ]; then
      kill $(cat "$pid_file") 2>/dev/null && log_ok "$app stopped" || log_warn "$app was not running"
      rm -f "$pid_file"
    fi
  done
}

# ── Status ─────────────────────────────────────────────────────
show_status() {
  log_header "Infrastructure (Docker)"
  require_docker
  docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps --format "table {{.Name}}\t{{.Status}}" 2>/dev/null || log_warn "Docker not running"

  log_header "Backend Services"
  for entry in "${BACKEND_SERVICES[@]}"; do
    IFS='|' read -r service port <<< "$entry"
    local pid_file="$PID_DIR/$service.pid"
    if [ -f "$pid_file" ] && kill -0 $(cat "$pid_file") 2>/dev/null; then
      echo -e "  ${GREEN}✅${NC} $service (port $port, PID: $(cat "$pid_file"), local)"
    elif curl -s --connect-timeout 1 http://localhost:$port/actuator/health >/dev/null 2>&1; then
      echo -e "  ${GREEN}✅${NC} $service (port $port, running)"
    else
      echo -e "  ${RED}❌${NC} $service (port $port, stopped)"
    fi
  done

  log_header "Docker Services"
  if [ ${#DOCKER_SERVICES[@]} -gt 0 ]; then
    for svc in "${DOCKER_SERVICES[@]}"; do
      if docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" ps "$svc" 2>/dev/null | grep -qi "up"; then
        echo -e "  ${GREEN}✅${NC} $svc"
      else
        echo -e "  ${RED}❌${NC} $svc"
      fi
    done
  else
    echo -e "  ${YELLOW}(none configured)${NC}"
  fi

  log_header "Web Applications"
  for app in dashboard frontend; do
    local pid_file="$PID_DIR/$app.pid"
    if [ -f "$pid_file" ] && kill -0 $(cat "$pid_file") 2>/dev/null; then
      local port=$([ "$app" = "dashboard" ] && echo "5173" || echo "5174")
      echo -e "  ${GREEN}✅${NC} $app (http://localhost:$port, PID: $(cat "$pid_file"))"
    else
      echo -e "  ${RED}❌${NC} $app (stopped)"
    fi
  done
  echo ""
}

# ── Logs ───────────────────────────────────────────────────────
show_logs() {
  local target=${1:-all}
  if [ "$target" = "all" ]; then
    tail -f $LOG_DIR/*.log 2>/dev/null || log_warn "No logs available"
  else
    tail -f "$LOG_DIR/$target.log" 2>/dev/null || log_warn "No logs for $target"
  fi
}

# ── Seed Data ──────────────────────────────────────────────────
seed_data() {
  log_info "Seeding test data..."
  curl -s -X POST http://localhost:8080/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@payflow.com","password":"Test@1234","firstName":"Admin","lastName":"User"}' | jq . >/dev/null 2>&1
  log_ok "Admin user created (admin@payflow.com / Test@1234)"
}

# ── Clean ──────────────────────────────────────────────────────
clean_all() {
  log_warn "Stopping everything..."
  services_stop_local
  web_stop
  infra_stop
  rm -rf "$LOG_DIR"
  log_ok "Everything cleaned"
}

# ── Help ───────────────────────────────────────────────────────
show_help() {
  cat <<EOF
${CYAN}PayFlow Smart Dev Environment Manager${NC}

${BLUE}Quick Start:${NC}
  ./scripts/dev.sh up              Start everything (infra Docker + services local + web)
  ./scripts/dev.sh up --docker     Start everything in Docker
  ./scripts/dev.sh status          Show status of all services
  ./scripts/dev.sh down            Stop everything

${BLUE}Infrastructure:${NC}
  infra:start      Start Postgres, Redis, Kafka in Docker
  infra:stop       Stop infrastructure

${BLUE}Backend Services (local with hot reload):${NC}
  services:start   Start all 6 backend services locally
  services:stop    Stop all backend services
  service:start <name>  Start single service (e.g., payment-service)
  service:stop <name>   Stop single service
  service:logs <name>   Tail service logs

${BLUE}Web Apps:${NC}
  web:start        Start dashboard (5173) and frontend (5174)
  web:stop         Stop web applications

${BLUE}Management:${NC}
  status           Show status of all services
  logs [name]      Show logs (all or specific)
  seed             Seed admin user and test data
  rebuild          Rebuild all services
  clean            Stop everything and remove logs
  help             Show this help

${BLUE}Examples:${NC}
  # Full local dev (recommended for development)
  ./scripts/dev.sh up

  # Full Docker (recommended for testing)
  ./scripts/dev.sh up --docker

  # Just infrastructure + one service
  ./scripts/dev.sh infra:start
  ./scripts/dev.sh service:start payment-service

  # Check status and tail logs
  ./scripts/dev.sh status
  ./scripts/dev.sh logs payment-service
EOF
}

# ── Main ───────────────────────────────────────────────────────
MODE="${2:-}"

case "${1:-help}" in
  up)
    if [ "$MODE" = "--docker" ]; then
      require_docker
      infra_start
      docker_start
    else
      require_docker
      infra_start
      services_start_local
      docker_start
      web_start
    fi
    sleep 5
    show_status
    ;;
  infra:start) infra_start ;;
  infra:stop) infra_stop ;;
  services:start) services_start_local ;;
  services:stop) services_stop_local ;;
  service:start)
    require_maven
    setup_env
    for entry in "${BACKEND_SERVICES[@]}"; do
      IFS='|' read -r service port <<< "$entry"
      if [ "$MODE" = "$service" ]; then
        start_local_service "$service" "$port"
        break
      fi
    done
    ;;
  service:stop)
    for entry in "${BACKEND_SERVICES[@]}"; do
      IFS='|' read -r service port <<< "$entry"
      if [ "$MODE" = "$service" ]; then
        stop_local_service "$service"
        break
      fi
    done
    ;;
  service:logs)
    show_logs "$MODE"
    ;;
  web:start) web_start ;;
  web:stop) web_stop ;;
  status) show_status ;;
  logs) show_logs "$MODE" ;;
  seed) seed_data ;;
  rebuild) require_maven; mvn clean package -DskipTests -q && log_ok "All services rebuilt" ;;
  down|clean) clean_all ;;
  help|*) show_help ;;
esac
