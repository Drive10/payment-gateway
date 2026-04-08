#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="$ROOT_DIR/.runlogs"
mkdir -p "$LOG_DIR"

start_frontend() {
  if lsof -nP -iTCP:5173 -sTCP:LISTEN >/dev/null 2>&1; then
    echo "[docker] frontend already running on :5173"
    return
  fi

  echo "[docker] starting frontend on :5173"
  nohup npm --prefix "$ROOT_DIR/web/frontend" run dev -- --host 0.0.0.0 --port 5173 \
    > "$LOG_DIR/frontend-docker.log" 2>&1 &
}

cd "$ROOT_DIR"

"$ROOT_DIR/scripts/dev-docker.sh"

"$ROOT_DIR/scripts/wait-for-health.sh" \
  http://localhost:8081 \
  http://localhost:8082 \
  http://localhost:8083 \
  http://localhost:8084 \
  http://localhost:8086 \
  http://localhost:8089 \
  http://localhost:8090 \
  http://localhost:8080

start_frontend

echo "[docker] ready"
echo "  frontend: http://localhost:5173"
echo "  gateway : http://localhost:8080"
