#!/bin/bash
# PayFlow startup diagnostic script
# Usage: ./scripts/diagnose.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}✓${NC} $1"; }
log_fail() { echo -e "${RED}✗${NC} $1"; }
log_warn() { echo -e "${YELLOW}⚠${NC} $1"; }
log_info() { echo -e "  $1"; }

echo "========================================"
echo "  PayFlow Startup Diagnostics"
echo "========================================"
echo ""

# Check Docker
echo "Checking Docker..."
if docker info >/dev/null 2>&1; then
    log_pass "Docker is running"
else
    log_fail "Docker is not running. Please start Docker."
    exit 1
fi

# Check Docker Compose
echo ""
echo "Checking Docker Compose..."
if docker compose version >/dev/null 2>&1; then
    log_pass "Docker Compose is available"
else
    log_fail "Docker Compose is not available"
    exit 1
fi

# Check required tools
echo ""
echo "Checking required tools..."
for tool in mvn npm nc; do
    if command -v "$tool" >/dev/null 2>&1; then
        log_pass "$tool is available"
    else
        log_warn "$tool is not installed"
    fi
done

# Check environment variables
echo ""
echo "Checking environment configuration..."
if [ -f .env ]; then
    log_pass ".env file exists"
    log_info "Loading environment..."
    export $(grep -v '^#' .env | xargs) 2>/dev/null || true
else
    log_warn ".env file not found"
fi

# Check infrastructure services
echo ""
echo "Checking infrastructure services..."
services=("mariadb" "redis" "mongodb" "kafka")
for svc in "${services[@]}"; do
    if docker compose ps "$svc" 2>/dev/null | grep -q "Up"; then
        log_pass "$svc is running"
    else
        log_warn "$svc is not running"
    fi
done

# Check ports
echo ""
echo "Checking port availability..."
ports=(3306 6379 27017 9092 8080 8081 8083 8084 8085 8086 8088 8089)
for port in "${ports[@]}"; do
    if ! nc -z localhost "$port" 2>/dev/null; then
        log_info "Port $port is available"
    else
        log_warn "Port $port is already in use"
    fi
done

# Check build artifacts
echo ""
echo "Checking build setup..."
if [ -f pom.xml ]; then
    log_pass "Maven project found"
else
    log_fail "Maven project not found"
fi

if [ -f web/frontend/package.json ]; then
    log_pass "Frontend project found"
else
    log_fail "Frontend project not found"
fi

# Check services are defined in docker-compose
echo ""
echo "Checking Docker Compose services..."
required_services=("mariadb" "redis" "auth-service" "api-gateway" "payment-service" "order-service")
for svc in "${required_services[@]}"; do
    if docker compose config 2>/dev/null | grep -q "^  $svc:"; then
        log_pass "$svc is defined"
    else
        log_warn "$svc is not defined"
    fi
done

echo ""
echo "========================================"
echo "  Diagnostics Complete"
echo "========================================"
echo ""
echo "To start PayFlow:"
echo "  make up          - Start all services"
echo "  make infra-up   - Start infrastructure only"
echo "  make build-all  - Build all Docker images"
echo ""