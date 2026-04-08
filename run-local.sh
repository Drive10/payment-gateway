#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/.runlogs"
mkdir -p "$LOG_DIR"

start_java_service() {
  local service_name="$1"
  local module_path="$2"
  local port="$3"
  local profile="${4:-local}"

  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "[local] $service_name already running on :$port"
    return
  fi

  echo "[local] starting $service_name (profile=$profile)"
  nohup mvn -pl "$module_path" spring-boot:run \
    -Dspring-boot.run.arguments="--spring.profiles.active=$profile" \
    > "$LOG_DIR/$service_name.log" 2>&1 &
}

start_frontend() {
  if lsof -nP -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "[local] frontend already running on :5173"
    return
  fi

  echo "[local] starting frontend on :5173"
  nohup npm --prefix "$ROOT_DIR/web/frontend" run dev -- --host 0.0.0.0 --port 5173 \
    > "$LOG_DIR/frontend.log" 2>&1 &
}

cd "$ROOT_DIR"

"$ROOT_DIR/scripts/dev-local.sh"

start_java_service "auth-service" "services/auth-service" 8081 local
start_java_service "order-service" "services/order-service" 8082 local
start_java_service "simulator-service" "services/simulator-service" 8086 local
start_java_service "notification-service" "services/notification-service" 8084 local
start_java_service "analytics-service" "services/analytics-service" 8089 local
start_java_service "audit-service" "services/audit-service" 8090 local
start_java_service "payment-service" "services/payment-service" 8083 local
start_java_service "api-gateway" "services/api-gateway" 8080 local

"$ROOT_DIR/scripts/wait-for-health.sh" \
  http://localhost:8081 \
  http://localhost:8082 \
  http://localhost:8086 \
  http://localhost:8084 \
  http://localhost:8089 \
  http://localhost:8090 \
  http://localhost:8083 \
  http://localhost:8080

start_frontend

echo "[local] ready"
echo "  frontend: http://localhost:5173"
echo "  gateway : http://localhost:8080"
