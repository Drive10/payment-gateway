#!/bin/bash
# E2E Test Runner for Local Development
# Runs Playwright tests against local services

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
FRONTEND_DIR="$PROJECT_ROOT/web/frontend"

echo "Running E2E tests..."

# Check if infrastructure is running
if ! docker compose ps postgres &>/dev/null; then
    echo "Starting infrastructure..."
    cd "$PROJECT_ROOT"
    docker compose up -d
    sleep 10
fi

# Run e2e tests
cd "$FRONTEND_DIR"

if [ "${1:-}" = "ui" ]; then
    echo "Running E2E tests with UI..."
    npm run test:e2e:ui
elif [ "${1:-}" = "headed" ]; then
    echo "Running E2E tests in headed mode..."
    npm run test:e2e:headed
else
    echo "Running E2E tests..."
    npm run test:e2e
fi

echo "E2E tests complete!"