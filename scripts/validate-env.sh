#!/bin/bash
# Environment validation script
# Usage: ./scripts/validate-env.sh

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_pass() { echo -e "${GREEN}✓${NC} $1"; }
log_fail() { echo -e "${RED}✗${NC} $1"; }
log_warn() { echo -e "${YELLOW}⚠${NC} $1"; }
log_info() { echo "  $1"; }

# Load env if exists
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs) 2>/dev/null || true
fi

echo "========================================"
echo "  Environment Validation"
echo "========================================"
echo ""

errors=0

# Check required variables
echo "Checking required environment variables..."

required_vars=(
    "DB_ROOT_PASSWORD"
    "REDIS_PASSWORD"
    "JWT_SECRET"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        log_warn "$var is not set (using default)"
    else
        log_pass "$var is set"
    fi
done

# Validate JWT_SECRET length
if [ -n "$JWT_SECRET" ]; then
    if [ ${#JWT_SECRET} -lt 32 ]; then
        log_fail "JWT_SECRET must be at least 32 characters"
        errors=$((errors + 1))
    else
        log_pass "JWT_SECRET is sufficiently long"
    fi
else
    log_warn "JWT_SECRET not set - using default (not secure for production!)"
fi

# Check Docker
echo ""
echo "Checking Docker..."
if docker info >/dev/null 2>&1; then
    log_pass "Docker is available"
else
    log_fail "Docker is not available"
    errors=$((errors + 1))
fi

# Check required ports
echo ""
echo "Checking port availability..."
ports_to_check=(3306 6379 27017 8080 8081)
for port in "${ports_to_check[@]}"; do
    if nc -z localhost "$port" 2>/dev/null; then
        log_warn "Port $port is already in use"
    else
        log_pass "Port $port is available"
    fi
done

echo ""
if [ $errors -eq 0 ]; then
    log_pass "Environment validation passed!"
    exit 0
else
    log_fail "Environment validation failed with $errors error(s)"
    exit 1
fi