#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "[docker] starting full stack..."
docker compose --env-file .env.docker up -d --build

echo "[docker] stack started"
echo "Gateway: http://localhost:8080"
