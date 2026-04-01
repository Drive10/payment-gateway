#!/bin/bash
# Payment Gateway - Development Mode Runner
# Usage: ./run-dev.sh [--start-infra] [--stop-infra] [--build] [--service <name>]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info() { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1"; }
log_success() { echo -e "${GREEN}[$(date '+%H:%M:%S')]${NC} $1"; }
log_error() { echo -e "${RED}[$(date '+%H:%M:%S')]${NC} $1"; }

# Detect OS
detect_os() {
    case "$(uname -s)" in
        Linux*)  echo "linux" ;;
        Darwin*) echo "mac" ;;
        CYGWIN*|MINGW*|MSYS*) echo "windows" ;;
        *)       echo "unknown" ;;
    esac
}

OS=$(detect_os)

start_infra() {
    log_info "Starting infrastructure services (PostgreSQL, Redis, Kafka)..."
    docker compose --profile infra up -d
    
    log_info "Waiting for infrastructure to be ready..."
    sleep 15
    
    log_info "Checking PostgreSQL..."
    if docker compose exec -T postgres pg_isready -U payment >/dev/null 2>&1; then
        log_success "PostgreSQL is ready"
    else
        log_error "PostgreSQL not ready"
    fi
    
    log_info "Checking Redis..."
    if docker compose exec -T redis redis-cli -a "${REDIS_PASSWORD:-password}" ping >/dev/null 2>&1; then
        log_success "Redis is ready"
    else
        log_error "Redis not ready"
    fi
    
    log_success "Infrastructure started!"
    echo ""
    echo -e "${YELLOW}Infrastructure Services:${NC}"
    echo "  - PostgreSQL: localhost:5433"
    echo "  - Redis: localhost:6379"
    echo "  - Kafka: localhost:9092"
}

stop_infra() {
    log_info "Stopping infrastructure services..."
    docker compose --profile infra down -v
    log_success "Infrastructure stopped!"
}

build_all() {
    log_info "Building all services..."
    mvn clean package -DskipTests
    log_success "All services built!"
}

run_service() {
    local service=$1
    local port
    local module
    
    case "$service" in
        config)    port=8888 ;;
        auth)      port=8081 ;;
        order)     port=8082 ;;
        payment)   port=8083 ;;
        notification) port=8084 ;;
        webhook)   port=8085 ;;
        simulator) port=8086 ;;
        settlement) port=8087 ;;
        risk)      port=8088 ;;
        analytics) port=8089 ;;
        merchant)  port=8090 ;;
        dispute)   port=8091 ;;
        gateway)   port=8080; module="api-gateway" ;;
        *)
            log_error "Unknown service: $service"
            echo "Available: config, auth, order, payment, notification, webhook, simulator, settlement, risk, analytics, merchant, dispute, gateway"
            exit 1
            ;;
    esac

    if [[ -z "$module" ]]; then
        module="${service}-service"
    fi
    
    log_info "Starting $service service on port $port..."
    cd "services/${module}"
    mvn spring-boot:run -Dspring-boot.run.profiles=local
}

show_help() {
    echo -e "${YELLOW}Payment Gateway - Dev Mode Runner${NC}"
    echo ""
    echo "Usage:"
    echo "  $0 --start-infra          Start infrastructure in Docker"
    echo "  $0 --stop-infra           Stop infrastructure"
    echo "  $0 --build                Build all services"
    echo "  $0 --service <name>       Run a specific service"
    echo "  $0 --help                 Show this help"
    echo ""
    echo "Example workflow:"
    echo "  1. $0 --start-infra"
    echo "  2. $0 --build"
    echo "  3. $0 --service auth      (in separate terminal)"
    echo "  4. $0 --service payment   (in separate terminal)"
    echo "  5. $0 --service gateway   (in separate terminal)"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --start-infra) start_infra; exit 0 ;;
        --stop-infra)  stop_infra; exit 0 ;;
        --build)       build_all; exit 0 ;;
        --service)     run_service "$2"; exit 0 ;;
        --help|-h)     show_help; exit 0 ;;
        *)             log_error "Unknown option: $1"; show_help; exit 1 ;;
    esac
done

show_help
