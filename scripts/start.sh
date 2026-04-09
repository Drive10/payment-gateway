#!/bin/bash

# PayFlow Development Startup Script
# Single command to start local development environment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# Check Docker
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker."
        exit 1
    fi
}

# Start infrastructure only
start_infra() {
    check_docker
    log_info "Starting infrastructure services..."
    docker compose --env-file .env up -d postgres mongodb redis zookeeper kafka zipkin
    log_info "Infrastructure started!"
    log_info "  - PostgreSQL: localhost:5432"
    log_info "  - MongoDB: localhost:27017"
    log_info "  - Redis: localhost:6379"
    log_info "  - Kafka: localhost:9092"
    log_info "  - Zipkin: localhost:9411"
}

# Start all services (for Docker mode - not used in local dev)
start_all() {
    check_docker
    log_info "Starting all services in Docker..."
    docker compose --env-file .env up -d
    log_info "All services started!"
}

# Stop infrastructure
stop_infra() {
    log_info "Stopping infrastructure..."
    docker compose --env-file .env down postgres mongodb redis zookeeper kafka zipkin
    log_info "Infrastructure stopped."
}

# Stop all services
stop_all() {
    log_info "Stopping all services..."
    docker compose --env-file .env down
    log_info "All services stopped."
}

# Show status
status() {
    echo ""
    echo "=== PayFlow Status ==="
    echo ""
    echo "Infrastructure (Docker):"
    docker compose --env-file .env ps postgres mongodb redis zookeeper kafka 2>/dev/null || true
    echo ""
    echo "Service Ports (for IntelliJ):"
    echo "  - auth-service:      8081"
    echo "  - order-service:     8082"
    echo "  - payment-service:   8083"
    echo "  - notification-svc:  8084"
    echo "  - simulator-service: 8086"
    echo "  - analytics-service: 8089"
    echo "  - audit-service:     8090"
    echo "  - api-gateway:       8080"
    echo ""
    echo "Frontend (VSCode):"
    echo "  - npm run dev:       localhost:5173"
    echo ""
}

# Show help
help() {
    echo "PayFlow Development Startup"
    echo ""
    echo "Usage: ./scripts/start.sh [command]"
    echo ""
    echo "Commands:"
    echo "  infra     Start infrastructure (PostgreSQL, MongoDB, Redis, Kafka)"
    echo "  start     Start all Docker services (full stack)"
    echo "  stop      Stop all services"
    echo "  status    Show service status and ports"
    echo "  clean     Stop all and remove volumes"
    echo "  help      Show this help"
    echo ""
    echo "=== Local Development Setup ==="
    echo ""
    echo "Terminal 1 (Infrastructure):"
    echo "  $ ./scripts/start.sh infra"
    echo ""
    echo "Terminal 2 (Backend - IntelliJ):"
    echo "  Run each service with: mvn spring-boot:run -pl services/<service-name>"
    echo "  Or use IntelliJ Run Configurations"
    echo ""
    echo "Terminal 3 (Frontend - VSCode):"
    echo "  $ cd web/frontend && npm run dev"
    echo ""
}

# Clean up everything
clean() {
    log_warn "This will remove all Docker volumes and data!"
    read -p "Are you sure? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker compose --env-file .env down -v
        log_info "All cleaned up!"
    else
        log_info "Cancelled."
    fi
}

# Main
case "${1:-help}" in
    infra)
        start_infra
        ;;
    start)
        start_all
        ;;
    stop)
        stop_all
        ;;
    status)
        status
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        help
        ;;
    *)
        log_error "Unknown command: $1"
        help
        exit 1
        ;;
esac