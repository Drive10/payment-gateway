#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/.runlogs"
mkdir -p "$LOG_DIR"

start_java_service() {
  local service_name="$1"
  local module_path="$2"
  local port="$3"
  local profile="${4:-hybrid}"

  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "[hybrid] $service_name already running on :$port"
    return
  fi

  echo "[hybrid] starting $service_name (profile=$profile)"
  nohup mvn -pl "$module_path" spring-boot:run \
    -Dspring-boot.run.arguments="--spring.profiles.active=$profile" \
    > "$LOG_DIR/$service_name-hybrid.log" 2>&1 &
}

start_frontend() {
  if lsof -nP -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "[hybrid] frontend already running on :5173"
    return
  fi

  echo "[hybrid] starting frontend on :5173"
  nohup npm --prefix "$ROOT_DIR/web/frontend" run dev -- --host 0.0.0.0 --port 5173 \
    > "$LOG_DIR/frontend-hybrid.log" 2>&1 &
}

cd "$ROOT_DIR"

"$ROOT_DIR/scripts/dev-hybrid.sh"

# In hybrid mode, auth/order run locally and gateway/payment/etc run in Docker.
start_java_service "auth-service" "services/auth-service" 8081 hybrid
start_java_service "order-service" "services/order-service" 8082 hybrid

"$ROOT_DIR/scripts/wait-for-health.sh" \
  http://localhost:8081 \
  http://localhost:8082 \
  http://localhost:8083 \
  http://localhost:8080

start_frontend

echo "[hybrid] ready"
echo "  frontend: http://localhost:5173"
echo "  gateway : http://localhost:8080"
