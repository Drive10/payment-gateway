#!/bin/bash

# Development helper script for PayFlow
# Usage: ./scripts/dev.sh [command]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
}

# Infrastructure management
infra_up() {
    check_docker
    log_info "Starting infrastructure services..."
    docker compose up -d postgres mongodb redis zookeeper kafka
    log_info "Infrastructure services started!"
    
    log_info "Waiting for services to be ready..."
    sleep 15
    
    log_info "Checking service status:"
    docker compose ps postgres mongodb redis zookeeper kafka
}

infra_down() {
    log_info "Stopping infrastructure services..."
    docker compose down postgres mongodb redis zookeeper kafka
    log_info "Infrastructure services stopped!"
}

# Backend services management
backend_start() {
    check_docker
    
    log_info "Starting backend services..."
    docker compose up -d
    log_info "Backend services started!"
    log_info "Checking service status:"
    docker compose ps
}

backend_stop() {
    log_info "Stopping backend services..."
    docker compose down
    log_info "Backend services stopped!"
}

# Frontend management
frontend_start() {
    log_info "Starting frontend development server..."
    cd "$PROJECT_ROOT/web/frontend"
    npm run dev
}

frontend_build() {
    log_info "Building frontend for production..."
    cd "$PROJECT_ROOT/web/frontend"
    npm run build
}

frontend_preview() {
    log_info "Previewing frontend production build..."
    cd "$PROJECT_ROOT/web/frontend"
    npm run preview
}

# Testing
run_tests() {
    check_docker
    
    log_info "Running backend tests..."
    cd "$PROJECT_ROOT"
    mvn test
    
    log_info "Running frontend tests..."
    cd "$PROJECT_ROOT/web/frontend"
    npm test
}

# Code quality
lint_code() {
    log_info "Running frontend code quality checks..."
    cd "$PROJECT_ROOT/web/frontend"
    npm run lint
}

format_code() {
    log_info "Formatting frontend code..."
    cd "$PROJECT_ROOT/web/frontend"
    npm run format
}

# Database management
db_reset() {
    log_info "Resetting database volumes..."
    docker compose down -v postgres mongodb
    docker compose up -d postgres mongodb
    log_info "Database volumes reset. Waiting for services to initialize..."
    sleep 10
}

# Help message
show_help() {
    echo "PayFlow Development Helper Script"
    echo ""
    echo "Usage: ./scripts/dev.sh [command]"
    echo ""
    echo "Commands:"
    echo "  infra:up          Start infrastructure services (PostgreSQL, MongoDB, Redis, Kafka)"
    echo "  infra:down        Stop infrastructure services"
    echo "  backend:start     Start all services via docker compose"
    echo "  backend:stop      Stop all services"
    echo "  frontend:start    Start frontend development server"
    echo "  frontend:build    Build frontend for production"
    echo "  frontend:preview  Preview frontend production build"
    echo "  test              Run all tests (backend and frontend)"
    echo "  lint              Run code quality checks"
    echo "  format            Format code according to project standards"
    echo "  db:reset          Reset database volumes (fresh data)"
    echo "  help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./scripts/dev.sh infra:up"
    echo "  ./scripts/dev.sh backend:start"
    echo "  ./scripts/dev.sh frontend:start"
}

# Main script logic
case "$1" in
    infra:up)
        infra_up
        ;;
    infra:down)
        infra_down
        ;;
    backend:start)
        backend_start
        ;;
    backend:stop)
        backend_stop
        ;;
    frontend:start)
        frontend_start
        ;;
    frontend:build)
        frontend_build
        ;;
    frontend:preview)
        frontend_preview
        ;;
    test)
        run_tests
        ;;
    lint)
        lint_code
        ;;
    format)
        format_code
        ;;
    db:reset)
        db_reset
        ;;
    help|*)
        show_help
        ;;
esac
