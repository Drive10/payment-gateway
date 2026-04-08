#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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

echo "[wait] http://localhost:5173/"
until curl -fsS "http://localhost:5173/" >/dev/null; do
  sleep 2
done

echo "[docker] ready"
echo "  frontend: http://localhost:5173"
echo "  gateway : http://localhost:8080"
