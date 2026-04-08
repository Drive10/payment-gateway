#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Local mode default: Vite frontend on 5173, local gateway on 8080.
E2E_REQUIRED_HEALTH_URLS="${E2E_REQUIRED_HEALTH_URLS:-http://localhost:8080,http://localhost:8081,http://localhost:8082,http://localhost:8083,http://localhost:8084,http://localhost:8086,http://localhost:8089,http://localhost:8090}" \
E2E_BASE_URL="${E2E_BASE_URL:-http://localhost:5173}" \
E2E_GATEWAY_URL="${E2E_GATEWAY_URL:-http://localhost:8080}" \
  "$ROOT_DIR/run-e2e.sh"
