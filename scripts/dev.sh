#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/web/frontend"
DASHBOARD_DIR="$REPO_ROOT/web/dashboard"
COMMAND="${1:-help}"
TARGET="${2:-}"
INFRA_SERVICES=(
  vault
  redis
  config-service
  kafka
  postgres
  mongodb
)
CORE_BACKEND_SERVICES=(
  auth-service
  order-service
  notification-service
  webhook-service
  simulator-service
  settlement-service
)
EXTENDED_BACKEND_SERVICES=(
  risk-service
  analytics-service
  merchant-service
  dispute-service
)
HYBRID_EDGE_SERVICES=(
  api-gateway
  frontend
  dashboard
)
FULL_ONLY_SERVICES=(
  payment-service
)
HYBRID_SERVICES=(
  vault
  redis
  config-service
  kafka
  postgres
  mongodb
  auth-service
  order-service
  notification-service
  webhook-service
  simulator-service
  settlement-service
  risk-service
  analytics-service
  merchant-service
  dispute-service
  api-gateway
  frontend
  dashboard
)

step() {
  echo
  echo "==> $1"
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

repo_cmd() {
  (
    cd "$REPO_ROOT"
    "$@"
  )
}

ui_cmd() {
  local dir="$1"
  shift
  (
    cd "$dir"
    "$@"
  )
}

ensure_ui_dependencies() {
  local dir="$1"
  if [[ ! -d "$dir/node_modules" ]]; then
    ui_cmd "$dir" npm ci
  fi
}

load_dotenv() {
  local env_file="$REPO_ROOT/.env"
  if [[ ! -f "$env_file" ]]; then
    return
  fi

  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "${line// }" ]] && continue
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" != *"="* ]] && continue

    local key="${line%%=*}"
    local value="${line#*=}"
    key="${key#"${key%%[![:space:]]*}"}"
    key="${key%"${key##*[![:space:]]}"}"
    value="${value%$'\r'}"

    if [[ "$value" == \"*\" && "$value" == *\" ]]; then
      value="${value:1:${#value}-2}"
    elif [[ "$value" == \'*\' ]]; then
      value="${value:1:${#value}-2}"
    fi

    if [[ -z "${!key:-}" ]]; then
      export "$key=$value"
    fi
  done < "$env_file"
}

compose_service_state() {
  local service="$1"
  local container_id=""
  container_id="$(cd "$REPO_ROOT" && docker compose ps -q "$service" 2>/dev/null | head -n 1 || true)"
  if [[ -z "$container_id" ]]; then
    echo "$service|missing|missing"
    return
  fi

  local inspect=""
  inspect="$(docker inspect --format '{{.State.Status}}|{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id" 2>/dev/null | head -n 1 || true)"
  if [[ -z "$inspect" ]]; then
    echo "$service|unknown|unknown"
    return
  fi

  echo "$service|$inspect"
}

wait_for_services() {
  local timeout_seconds="${1:-1200}"
  shift
  local services=("$@")
  local deadline=$((SECONDS + timeout_seconds))
  local last_summary=""

  while (( SECONDS < deadline )); do
    local pending=()
    for service in "${services[@]}"; do
      IFS='|' read -r _ state health <<<"$(compose_service_state "$service")"
      if [[ "$state" != "running" ]]; then
        pending+=("$service ($state)")
        continue
      fi
      if [[ "$health" == "healthy" || "$health" == "none" ]]; then
        continue
      fi
      pending+=("$service ($health)")
    done

    if (( ${#pending[@]} == 0 )); then
      return
    fi

    local summary="${pending[*]}"
    if [[ "$summary" != "$last_summary" ]]; then
      echo "Waiting for: ${pending[*]}"
      last_summary="$summary"
    fi
    sleep 15
  done

  echo "Timed out waiting for Docker services: ${services[*]}" >&2
  exit 1
}

start_compose_wave() {
  local timeout_seconds="$1"
  local build_flag="$2"
  local remove_orphans_flag="$3"
  local payment_service_url="$4"
  shift 4

  local profiles=()
  while [[ "${1:-}" == profile:* ]]; do
    profiles+=("${1#profile:}")
    shift
  done

  local services=("$@")
  local args=(compose)
  local profile
  for profile in "${profiles[@]}"; do
    args+=(--profile "$profile")
  done
  args+=(up -d)
  if [[ "$build_flag" == "true" ]]; then
    args+=(--build)
  fi
  if [[ "$remove_orphans_flag" == "true" ]]; then
    args+=(--remove-orphans)
  fi
  args+=("${services[@]}")

  (
    if [[ -n "$payment_service_url" ]]; then
      export PAYMENT_SERVICE_URL="$payment_service_url"
    fi
    cd "$REPO_ROOT"
    docker "${args[@]}"
  )

  wait_for_services "$timeout_seconds" "${services[@]}"
}

show_help() {
  cat <<'EOF'
Usage: ./scripts/dev.sh <command> [service-name]

Commands:
  bootstrap       Prepare local defaults and install UI dependencies when Node is available
  doctor          Validate Java, Maven wrapper, Docker, and optional Node/npm
  infra           Start Docker infrastructure only
  hybrid          Start Docker platform except payment-service
  full            Start the full Docker stack
  docker          Alias for full
  local           Start infra and run payment-service locally
  build           Build all backend services
  build-service   Build a single backend service
  status          Show the current Docker status
  down            Stop the active Docker stack
  service-local   Run a single backend service locally (example: service-local payment-service)
  payment-local   Alias for service-local payment-service
  frontend-check  Run frontend and dashboard quality checks
  smoke           Run compose validation, backend smoke tests, and UI builds
  test-all        Run verify plus UI checks
  verify          Run backend verification
  compose-check   Validate infra, full, and hybrid Docker rendering
  help            Show this help
EOF
}

bootstrap() {
  step "Preparing local workspace"
  if [[ ! -f "$REPO_ROOT/.env" && -f "$REPO_ROOT/.env.example" ]]; then
    cp "$REPO_ROOT/.env.example" "$REPO_ROOT/.env"
    echo "Created .env from .env.example"
  elif [[ -f "$REPO_ROOT/.env" ]]; then
    echo ".env already exists"
  fi

  if has_cmd node && has_cmd npm; then
    step "Installing UI dependencies"
    ensure_ui_dependencies "$FRONTEND_DIR"
    ensure_ui_dependencies "$DASHBOARD_DIR"
  else
    echo "Skipping UI dependency install because node/npm are not available."
  fi
}

doctor() {
  step "Checking local toolchain"
  local failed=0

  if has_cmd java; then
    echo "java       OK"
  else
    echo "java       MISSING"
    failed=1
  fi

  if has_cmd docker; then
    echo "docker     OK"
    if docker info >/dev/null 2>&1; then
      echo "dockerd    OK"
    else
      echo "dockerd    NOT RUNNING"
      failed=1
    fi
  else
    echo "docker     MISSING"
    failed=1
  fi

  if has_cmd node; then
    echo "node       OK"
  else
    echo "node       OPTIONAL"
  fi

  if has_cmd npm; then
    echo "npm        OK"
  else
    echo "npm        OPTIONAL"
  fi

  if [[ -f "$REPO_ROOT/.env" ]]; then
    echo ".env       OK"
  else
    echo ".env       MISSING"
  fi

  if [[ "$failed" -ne 0 ]]; then
    exit 1
  fi
}

infra() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Starting Docker infrastructure"
  start_compose_wave 600 false true "" profile:infra "${INFRA_SERVICES[@]}"
  echo "Infrastructure is ready."
}

full() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Starting full Docker stack"
  start_compose_wave 600 false true "" profile:services profile:full "${INFRA_SERVICES[@]}"
  start_compose_wave 900 true false "" profile:services profile:full "${CORE_BACKEND_SERVICES[@]}"
  start_compose_wave 1200 true false "" profile:services profile:full "${FULL_ONLY_SERVICES[@]}"
  start_compose_wave 1200 true false "" profile:services profile:full "${EXTENDED_BACKEND_SERVICES[@]}"
  start_compose_wave 900 true false "" profile:services profile:full "${HYBRID_EDGE_SERVICES[@]}"
  echo "Frontend:  http://localhost:3000"
  echo "Dashboard: http://localhost:3001"
  echo "Gateway:   http://localhost:8080"
}

hybrid() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Starting hybrid Docker stack without payment-service"
  local hybrid_payment_url="http://host.docker.internal:8083"
  start_compose_wave 600 false true "$hybrid_payment_url" profile:services "${INFRA_SERVICES[@]}"
  start_compose_wave 900 true false "$hybrid_payment_url" profile:services "${CORE_BACKEND_SERVICES[@]}"
  start_compose_wave 1200 true false "$hybrid_payment_url" profile:services "${EXTENDED_BACKEND_SERVICES[@]}"
  start_compose_wave 900 true false "$hybrid_payment_url" profile:services "${HYBRID_EDGE_SERVICES[@]}"
  echo "Docker services are up without payment-service."
  echo "Run local payment-service with: ./scripts/dev.sh payment-local"
}

local_mode() {
  infra
  echo "Starting payment-service locally..."
  service_local "payment-service"
}

build() {
  step "Building backend services"
  repo_cmd ./mvnw clean package -DskipTests
}

build_service() {
  local service="${1:-payment-service}"
  step "Building $service"
  repo_cmd ./mvnw clean package -DskipTests -pl "services/$service" -am
}

status() {
  step "Service Status"
  repo_cmd docker compose ps
}

down() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Stopping Docker stack"
  repo_cmd docker compose --profile services --profile full --profile infra --profile observability --profile advanced down --remove-orphans
}

service_local() {
  local service_name="${1:-}"
  if [[ -z "$service_name" ]]; then
    echo "Provide a service name, for example: ./scripts/dev.sh service-local payment-service" >&2
    exit 1
  fi

  local service_dir="$REPO_ROOT/services/$service_name"
  if [[ ! -d "$service_dir" ]]; then
    echo "Unknown service: $service_name" >&2
    exit 1
  fi
  local service_pom="$service_dir/pom.xml"
  if [[ ! -f "$service_pom" ]]; then
    echo "Service pom.xml not found for $service_name" >&2
    exit 1
  fi

  local profile=""
  if [[ -f "$service_dir/src/main/resources/application-local.yml" ]]; then
    profile="local"
  elif [[ -f "$service_dir/src/main/resources/application-dev.yml" ]]; then
    profile="dev"
  fi

  local target_port=""
  case "$service_name" in
    api-gateway) target_port="8080" ;;
    auth-service) target_port="8081" ;;
    order-service) target_port="8082" ;;
    payment-service) target_port="8083" ;;
    notification-service) target_port="8084" ;;
    webhook-service) target_port="8085" ;;
    simulator-service) target_port="8086" ;;
    settlement-service) target_port="8087" ;;
    risk-service) target_port="8088" ;;
    analytics-service) target_port="8089" ;;
    merchant-service) target_port="8090" ;;
    dispute-service) target_port="8091" ;;
    config-service) target_port="8888" ;;
  esac

  load_dotenv
  export DB_HOST="${DB_HOST:-localhost}"
  export POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
  export DB_PORT="${DB_PORT:-5433}"
  export POSTGRES_PORT="${POSTGRES_PORT:-5433}"
  export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}"
  export REDIS_HOST="${REDIS_HOST:-localhost}"
  export REDIS_PORT="${REDIS_PORT:-6379}"
  export WEBHOOK_SERVICE_URL="${WEBHOOK_SERVICE_URL:-http://localhost:8085}"
  export ORDER_SERVICE_URL="${ORDER_SERVICE_URL:-http://localhost:8082}"
  export PAYMENT_SERVICE_URL="${PAYMENT_SERVICE_URL:-http://localhost:8083}"
  export SIMULATOR_SERVICE_URL="${SIMULATOR_SERVICE_URL:-http://localhost:8086}"
  export AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8081}"
  export MERCHANT_SERVICE_URL="${MERCHANT_SERVICE_URL:-http://localhost:8090}"
  export DISPUTE_SERVICE_URL="${DISPUTE_SERVICE_URL:-http://localhost:8091}"

  case "$service_name" in
    auth-service)
      export DB_USERNAME="${DB_USERNAME:-${AUTH_DB_USER:-auth}}"
      export DB_PASSWORD="${DB_PASSWORD:-${AUTH_DB_PASSWORD:-authpass}}"
      export DB_NAME="${DB_NAME:-authdb}"
      ;;
    notification-service)
      export DB_USERNAME="${DB_USERNAME:-${NOTIFICATION_DB_USER:-notification}}"
      export DB_PASSWORD="${DB_PASSWORD:-${NOTIFICATION_DB_PASSWORD:-notificationpass}}"
      export DB_NAME="${DB_NAME:-notificationdb}"
      ;;
    webhook-service)
      export DB_USERNAME="${DB_USERNAME:-${WEBHOOK_DB_USER:-webhook}}"
      export DB_PASSWORD="${DB_PASSWORD:-${WEBHOOK_DB_PASSWORD:-webhookpass}}"
      export DB_NAME="${DB_NAME:-webhookdb}"
      ;;
    simulator-service)
      export DB_USERNAME="${DB_USERNAME:-${SIMULATOR_DB_USER:-simulator}}"
      export DB_PASSWORD="${DB_PASSWORD:-${SIMULATOR_DB_PASSWORD:-simulatorpass}}"
      export DB_NAME="${DB_NAME:-simulatordb}"
      ;;
    settlement-service)
      export DB_USERNAME="${DB_USERNAME:-${SETTLEMENT_DB_USER:-settlement}}"
      export DB_PASSWORD="${DB_PASSWORD:-${SETTLEMENT_DB_PASSWORD:-settlementpass}}"
      export DB_NAME="${DB_NAME:-settlementdb}"
      ;;
    risk-service)
      export DB_USERNAME="${DB_USERNAME:-${RISK_DB_USER:-risk}}"
      export DB_PASSWORD="${DB_PASSWORD:-${RISK_DB_PASSWORD:-riskpass}}"
      export DB_NAME="${DB_NAME:-riskdb}"
      ;;
    analytics-service)
      export DB_USERNAME="${DB_USERNAME:-${ANALYTICS_DB_USER:-analytics}}"
      export DB_PASSWORD="${DB_PASSWORD:-${ANALYTICS_DB_PASSWORD:-analyticspass}}"
      export DB_NAME="${DB_NAME:-analyticsdb}"
      ;;
    merchant-service)
      export DB_USERNAME="${DB_USERNAME:-${MERCHANT_DB_USER:-merchant}}"
      export DB_PASSWORD="${DB_PASSWORD:-${MERCHANT_DB_PASSWORD:-merchantpass}}"
      export DB_NAME="${DB_NAME:-merchantdb}"
      ;;
    dispute-service)
      export DB_USERNAME="${DB_USERNAME:-${DISPUTE_DB_USER:-dispute}}"
      export DB_PASSWORD="${DB_PASSWORD:-${DISPUTE_DB_PASSWORD:-disputepass}}"
      export DB_NAME="${DB_NAME:-disputedb}"
      ;;
    payment-service)
      export DB_USERNAME="${DB_USERNAME:-${PAYMENT_DB_USER:-payment}}"
      export DB_PASSWORD="${DB_PASSWORD:-${POSTGRES_PASSWORD:-${PAYMENT_DB_PASSWORD:-paymentpass}}}"
      export DB_NAME="${DB_NAME:-paymentdb}"
      ;;
  esac

  step "Running $service_name locally"
  if has_cmd docker; then
    local container_id=""
    container_id="$(cd "$REPO_ROOT" && docker compose ps -q "$service_name" 2>/dev/null || true)"
    if [[ -n "$container_id" ]]; then
      echo "Stopping Docker container for $service_name so the local process can bind its port."
      repo_cmd docker compose stop "$service_name" >/dev/null
    fi
  fi
  if [[ -n "$target_port" ]] && has_cmd lsof; then
    while IFS= read -r pid; do
      [[ -z "$pid" ]] && continue
      local proc_name=""
      proc_name="$(ps -p "$pid" -o comm= 2>/dev/null || true)"
      if [[ "$proc_name" == *java* ]]; then
        echo "Stopping stale local Java process on port $target_port before starting $service_name."
        kill -9 "$pid" >/dev/null 2>&1 || true
      fi
    done < <(lsof -ti "tcp:$target_port" -sTCP:LISTEN 2>/dev/null || true)
  fi
  (
    cd "$REPO_ROOT"
    if [[ -n "$profile" ]]; then
      SPRING_PROFILES_ACTIVE="$profile" ./mvnw -f "$service_pom" clean spring-boot:run "-Dspring-boot.run.profiles=$profile"
    else
      ./mvnw -f "$service_pom" clean spring-boot:run
    fi
  )
}

frontend_check() {
  require_cmd node "Install Node.js 22 and add it to PATH."
  require_cmd npm "Install Node.js and npm before working on the frontend."

  step "Running frontend checks"
  ensure_ui_dependencies "$FRONTEND_DIR"
  ui_cmd "$FRONTEND_DIR" npm run check

  step "Running dashboard checks"
  ensure_ui_dependencies "$DASHBOARD_DIR"
  ui_cmd "$DASHBOARD_DIR" npm run lint
  ui_cmd "$DASHBOARD_DIR" npm run build
}

verify() {
  step "Running backend verification"
  repo_cmd ./mvnw -q verify
}

compose_check() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Validating compose rendering"
  repo_cmd docker compose --profile infra config >/dev/null
  repo_cmd docker compose --profile services config >/dev/null
  repo_cmd docker compose --profile services --profile full config >/dev/null
  (
    export PAYMENT_SERVICE_URL="http://host.docker.internal:8083"
    cd "$REPO_ROOT"
    docker compose --profile services config >/dev/null
  )
}

smoke() {
  step "Running smoke checks"
  compose_check
  repo_cmd ./mvnw -q -pl services/payment-service,services/api-gateway -am test

  if has_cmd node && has_cmd npm; then
    frontend_check
  else
    echo "Skipping UI checks because node/npm are not available."
  fi
}

test_all() {
  step "Running full verification"
  compose_check
  verify

  if has_cmd node && has_cmd npm; then
    frontend_check
  else
    echo "Skipping UI checks because node/npm are not available."
  fi
}

case "$COMMAND" in
  bootstrap) bootstrap ;;
  doctor) doctor ;;
  infra) infra ;;
  docker) full ;;
  hybrid) hybrid ;;
  local) local_mode ;;
  build) build ;;
  build-service) build_service "$TARGET" ;;
  full) full ;;
  status) status ;;
  down) down ;;
  service-local) service_local "$TARGET" ;;
  payment-local) service_local "payment-service" ;;
  frontend-check) frontend_check ;;
  smoke) smoke ;;
  test-all) test_all ;;
  verify) verify ;;
  compose-check) compose_check ;;
  help) show_help ;;
  *)
    show_help
    exit 1
    ;;
esac
