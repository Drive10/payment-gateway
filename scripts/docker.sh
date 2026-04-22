#!/bin/bash
# =============================================================================
# PayFlow Full Docker Script
# Runs entire stack in Docker containers
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

log()   { echo -e "${GREEN}[PayFlow]${NC} $1"; }
info()  { echo -e "${CYAN}[PayFlow]${NC} $1"; }
warn()  { echo -e "${YELLOW}[PayFlow]${NC} $1"; }
error() { echo -e "${RED}[PayFlow]${NC} $1"; }

start() {
    log "Starting full stack in Docker..."
    
    # Build images if needed
    if [ "$1" == "--build" ]; then
        log "Building Docker images..."
        docker compose -f docker-compose.full.yml build --no-cache
    fi
    
    # Start all services
    docker compose -f docker-compose.full.yml up -d --wait
    
    log "Waiting for services to be healthy..."
    local timeout=120
    local count=0
    
    local services=("api-gateway:8080" "auth-service:8081" "order-service:8082" "payment-service:8083")
    
    for svc in "${services[@]}"; do
        service="${svc%%:*}"
        port="${svc##*:}"
        
        while [ $count -lt $timeout ]; do
            if docker inspect --format='{{.State.Health.Status}}' $service 2>/dev/null | grep -q "healthy"; then
                log "$service is healthy"
                break
            fi
            sleep 2
            count=$((count + 2))
        done
    done
    
    show_status
}

stop() {
    log "Stopping Docker stack..."
    docker compose -f docker-compose.full.yml down
    log "Stack stopped"
}

logs() {
    if [ -z "$2" ]; then
        docker compose -f docker-compose.full.yml logs -f
    else
        docker compose -f docker-compose.full.yml logs -f "$2"
    fi
}

status() {
    echo ""
    docker compose -f docker-compose.full.yml ps
    echo ""
    echo "Service URLs:"
    echo "  API Gateway:    http://localhost:8080"
    echo "  Auth:        http://localhost:8081"
    echo "  Order:       http://localhost:8082"
    echo "  Payment:     http://localhost:8083"
    echo "  Notification: http://localhost:8084"
    echo "  Simulator:   http://localhost:8086"
}

show_status() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  PayFlow Docker Stack Ready!${NC}"
    echo -e "${BLUE}========================================${NC}"
    status
    echo ""
    info "View logs: ./scripts/docker.sh logs"
    info "Stop: ./scripts/docker.sh stop"
}

case "${1:-start}" in
    start)
        start "${2:-}"
        ;;
    stop)
        stop
        ;;
    restart)
        stop
        sleep 2
        start
        ;;
    logs)
        logs "$@"
        ;;
    status)
        status
        ;;
    build)
        docker compose -f docker-compose.full.yml build
        ;;
    help|--help|-h)
        echo "PayFlow Full Docker"
        echo ""
        echo "Usage: $0 {start|stop|restart|logs|status|build}"
        echo ""
        echo "Commands:"
        echo "  start     - Start stack (use --build to rebuild)"
        echo "  stop      - Stop stack"
        echo "  restart   - Restart stack"
        echo "  logs     - View logs"
        echo "  status   - Show status"
        echo "  build    - Build images"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use: $0 help"
        exit 1
        ;;
esac