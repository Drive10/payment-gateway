#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$REPO_ROOT/services/frontend"
COMMAND="${1:-help}"

step() {
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

show_help() {
  cat <<'EOF'
Usage: ./scripts/dev.sh <command>

Commands:
  bootstrap       Prepare local repo defaults and optional frontend deps
  doctor          Validate Java, Maven, Docker, and optional Node/npm
  hybrid          Start Docker services for hybrid mode
  full            Start the full Docker stack
  down            Stop the active Docker stack
  payment-local   Run payment-service locally with the local profile
  frontend-check  Run frontend install + quality checks
  smoke           Run fast local smoke checks
  verify          Run backend verification
  compose-check   Validate Compose rendering for hybrid and full modes
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
    step "Installing frontend dependencies"
    cd "$FRONTEND_DIR"
    npm ci
  else
    echo "Skipping frontend install because node/npm are not available."
  fi

  echo "Next steps:"
  echo "  1. ./scripts/dev.sh doctor"
  echo "  2. ./scripts/dev.sh hybrid"
  echo "  3. ./scripts/dev.sh payment-local"
}

doctor() {
  step "Checking local toolchain"
  local failed=0
  for name in java docker; do
    if has_cmd "$name"; then
      printf "%-10s OK\n" "$name"
    else
      printf "%-10s MISSING\n" "$name"
      failed=1
    fi
  done

  for name in mvn node npm; do
    if has_cmd "$name"; then
      printf "%-10s OK\n" "$name"
    else
      printf "%-10s MISSING\n" "$name"
    fi
  done

  if [[ ! -f "$REPO_ROOT/.env" ]]; then
    echo ".env       MISSING"
    echo "           Copy .env.example to .env before running the platform."
  else
    echo ".env       OK"
  fi

  if [[ "$failed" -ne 0 ]]; then
    echo "Required tooling is missing." >&2
    exit 1
  fi
}

hybrid() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Starting hybrid Docker stack"
  cd "$REPO_ROOT"
  docker compose -f docker-compose.yml -f docker-compose.override.yml --profile services --profile optional up -d --build
}

full() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Starting full Docker stack"
  cd "$REPO_ROOT"
  docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services --profile full --profile optional up -d --build
}

down() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Stopping Docker stack"
  cd "$REPO_ROOT"
  docker compose down
}

payment_local() {
  step "Running payment-service locally"
  cd "$REPO_ROOT"
  SPRING_PROFILES_ACTIVE=local ./mvnw -pl services/payment-service -am spring-boot:run
}

frontend_check() {
  require_cmd node "Install the Node.js version pinned in .nvmrc."
  require_cmd npm "Install Node.js and npm before working on the frontend."
  step "Running frontend quality checks"
  cd "$FRONTEND_DIR"
  if [[ ! -x "$FRONTEND_DIR/node_modules/.bin/eslint" ]]; then
    npm ci
  fi
  npm run check
}

verify_repo() {
  step "Running backend verification"
  cd "$REPO_ROOT"
  ./mvnw -q verify
}

compose_check() {
  require_cmd docker "Install Docker Desktop and ensure the daemon is running."
  step "Validating Compose files"
  cd "$REPO_ROOT"
  docker compose -f docker-compose.yml -f docker-compose.override.yml --profile services config >/dev/null
  docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services --profile full --profile optional config >/dev/null
}

smoke() {
  step "Running fast smoke checks"
  compose_check
  cd "$REPO_ROOT"
  ./mvnw -q -pl services/payment-service,services/ledger-service,services/api-gateway -am test
  if has_cmd node && has_cmd npm; then
    step "Running frontend lint"
    cd "$FRONTEND_DIR"
    if [[ ! -x "$FRONTEND_DIR/node_modules/.bin/eslint" ]]; then
      npm ci
    fi
    npm run lint
  else
    echo "Skipping frontend lint because node/npm are not available."
  fi
}

case "$COMMAND" in
  bootstrap) bootstrap ;;
  doctor) doctor ;;
  hybrid) hybrid ;;
  full) full ;;
  down) down ;;
  payment-local) payment_local ;;
  frontend-check) frontend_check ;;
  smoke) smoke ;;
  verify) verify_repo ;;
  compose-check) compose_check ;;
  help) show_help ;;
  *)
    show_help
    echo "Unknown command: $COMMAND" >&2
    exit 1
    ;;
esac
