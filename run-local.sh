#!/bin/bash
# ===========================================
# PayFlow Local Development Launcher
# Starts all backend services + frontend in parallel
# ===========================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

SERVICES=(
    "api-gateway:8080"
    "auth-service:8081"
    "order-service:8082"
    "payment-service:8083"
    "notification-service:8084"
    "simulator-service:8086"
    "analytics-service:8089"
    "audit-service:8090"
)

log() { echo -e "${GREEN}[PayFlow]${NC} $1"; }
warn() { echo -e "${YELLOW}[PayFlow]${NC} $1"; }
info() { echo -e "${CYAN}[PayFlow]${NC} $1"; }

cleanup() {
    log "Stopping all services..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true
    log "All services stopped"
    exit 0
}

trap cleanup SIGINT SIGTERM

check_infra() {
    log "Checking infrastructure..."
    local missing=()
    
    for port in 5432 6379 27017; do
        if ! lsof -i :$port >/dev/null 2>&1; then
            case $port in
                5432) missing+=("PostgreSQL") ;;
                6379) missing+=("Redis") ;;
                27017) missing+=("MongoDB") ;;
            esac
        fi
    done
    
    if [ ${#missing[@]} -gt 0 ]; then
        warn "Missing infra: ${missing[*]}"
        info "Run 'make infra-up' to start Docker infrastructure"
        read -p "Continue anyway? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        log "Infrastructure is running"
    fi
}

start_services() {
    log "Starting backend services..."
    
    for svc in "${SERVICES[@]}"; do
        service="${svc%%:*}"
        port="${svc##*:}"
        
        info "Starting $service on port $port..."
        nohup mvn spring-boot:run -pl services/$service -Dspring-boot.run.profiles=local -q > /tmp/$service.log 2>&1 &
    done
    
    log "All backend services started (check logs in /tmp/<service>.log)"
}

start_frontend() {
    info "Starting frontend..."
    cd "$SCRIPT_DIR/web/payment-page"
    nohup npm run dev > /tmp/frontend.log 2>&1 &
    cd "$SCRIPT_DIR"
}

wait_for_services() {
    log "Waiting for services to be ready..."
    
    local timeout=120
    local count=0
    
    for svc in "${SERVICES[@]}"; do
        service="${svc%%:*}"
        port="${svc##*:}"
        
        while ! curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; do
            if [ $count -gt $timeout ]; then
                warn "$service not ready after ${timeout}s (check /tmp/$service.log)"
            fi
            sleep 2
            count=$((count + 2))
        done
        log "$service is ready"
    done
}

show_status() {
    echo ""
    echo -e "${BLUE}===========================================${NC}"
    echo -e "${BLUE}  PayFlow Services Ready!${NC}"
    echo -e "${BLUE}===========================================${NC}"
    echo -e "  ${GREEN}Frontend:${NC}    http://localhost:5173"
    echo -e "  ${GREEN}API Gateway:${NC} http://localhost:8080"
    echo -e "  ${GREEN}Auth Service:${NC} http://localhost:8081"
    echo -e "  ${GREEN}Order Service:${NC} http://localhost:8082"
    echo -e "  ${GREEN}Payment:${NC}     http://localhost:8083"
    echo -e "  ${GREEN}Notification:${NC} http://localhost:8084"
    echo -e "  ${GREEN}Simulator:${NC}  http://localhost:8086"
    echo -e "  ${GREEN}Analytics:${NC} http://localhost:8089"
    echo -e "  ${GREEN}Audit:${NC}      http://localhost:8090"
    echo ""
    info "View logs: tail -f /tmp/<service>.log"
    info "Stop all: pkill -f 'spring-boot:run' (or Ctrl+C)"
}

case "${1:-start}" in
    start)
        check_infra
        start_services
        start_frontend
        wait_for_services
        show_status
        wait
        ;;
    stop)
        cleanup
        ;;
    restart)
        pkill -f "spring-boot:run" 2>/dev/null || true
        pkill -f "vite" 2>/dev/null || true
        sleep 2
        start_services
        start_frontend
        wait_for_services
        show_status
        wait
        ;;
    logs)
        if [ -z "$2" ]; then
            tail -f /tmp/api-gateway.log
        else
            tail -f "/tmp/$2.log" 2>/dev/null || echo "Log not found: /tmp/$2.log"
        fi
        ;;
    status)
        for svc in "${SERVICES[@]}"; do
            service="${svc%%:*}"
            port="${svc##*:}"
            if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
                echo -e "${GREEN}✓${NC} $service (port $port)"
            else
                echo -e "${RED}✗${NC} $service (port $port) - not running"
            fi
        done
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|logs|status}"
        echo ""
        echo "Commands:"
        echo "  start     - Start all services (default)"
        echo "  stop      - Stop all services"
        echo "  restart   - Restart all services"
        echo "  logs      - View logs (optional: service name)"
        echo "  status    - Check service status"
        exit 1
        ;;
esac