#!/bin/bash
# ===========================================
# PayFlow Individual Service Launcher
# Start/stop individual services for development
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
error() { echo -e "${RED}[PayFlow]${NC} $1"; }

show_help() {
    echo "Usage: $0 <command> [service]"
    echo ""
    echo "Commands:"
    echo "  start <service>  - Start a single service"
    echo "  stop <service>   - Stop a single service"
    echo "  start-all        - Start all services"
    echo "  stop-all         - Stop all services"
    echo "  status           - Show all service status"
    echo "  logs <service>   - Show logs for a service"
    echo "  restart <service>- Restart a service"
    echo ""
    echo "Available services:"
    for svc in "${SERVICES[@]}"; do
        name="${svc%%:*}"
        port="${svc##*:}"
        echo "  - $name (port $port)"
    done
    echo ""
    echo "Examples:"
    echo "  $0 start auth-service    # Start auth service"
    echo "  $0 stop payment-service  # Stop payment service"
    echo "  $0 logs payment          # View payment service logs"
}

find_service() {
    local input="$1"
    for svc in "${SERVICES[@]}"; do
        name="${svc%%:*}"
        if [[ "$name" == *"$input"* ]]; then
            echo "$svc"
            return 0
        fi
    done
    return 1
}

start_service() {
    local service="$1"
    local port="$2"
    
    if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
        log "$service is already running on port $port"
        return 0
    fi
    
    log "Starting $service..."
    nohup mvn spring-boot:run -pl services/$service -q > /tmp/$service.log 2>&1 &
    local pid=$!
    
    local timeout=60
    local count=0
    while ! curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; do
        if [ $count -gt $timeout ]; then
            error "$service failed to start (check /tmp/$service.log)"
            return 1
        fi
        sleep 2
        count=$((count + 2))
    done
    
    log "$service started successfully (PID: $pid)"
}

stop_service() {
    local service="$1"
    local pids=$(pgrep -f "spring-boot:run.*services/$service" 2>/dev/null || true)
    
    if [ -z "$pids" ]; then
        warn "$service is not running"
        return 0
    fi
    
    log "Stopping $service (PIDs: $pids)..."
    pkill -f "spring-boot:run.*services/$service" 2>/dev/null || true
    sleep 2
    log "$service stopped"
}

start_all() {
    log "Starting all services..."
    for svc in "${SERVICES[@]}"; do
        service="${svc%%:*}"
        port="${svc##*:}"
        start_service "$service" "$port" &
    done
    
    wait
    log "All services started"
    status
}

stop_all() {
    log "Stopping all services..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    sleep 2
    log "All services stopped"
}

show_status() {
    echo ""
    echo -e "${BLUE}Service Status${NC}"
    echo "===================="
    for svc in "${SERVICES[@]}"; do
        name="${svc%%:*}"
        port="${svc##*:}"
        if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
            echo -e "  ${GREEN}✓${NC} $name (port $port)"
        else
            echo -e "  ${RED}○${NC} $name (port $port)"
        fi
    done
}

show_logs() {
    local service="$1"
    if [ -f "/tmp/$service.log" ]; then
        tail -f "/tmp/$service.log"
    else
        error "No logs found for $service (not started?)"
    fi
}

COMMAND="${1:-}"
SERVICE_ARG="${2:-}"

case "$COMMAND" in
    start)
        if [ -z "$SERVICE_ARG" ]; then
            error "Service name required"
            show_help
            exit 1
        fi
        svc=$(find_service "$SERVICE_ARG") || { error "Unknown service: $SERVICE_ARG"; exit 1; }
        service="${svc%%:*}"
        port="${svc##*:}"
        start_service "$service" "$port"
        ;;
    stop)
        if [ -z "$SERVICE_ARG" ]; then
            error "Service name required"
            show_help
            exit 1
        fi
        svc=$(find_service "$SERVICE_ARG") || { error "Unknown service: $SERVICE_ARG"; exit 1; }
        service="${svc%%:*}"
        stop_service "$service"
        ;;
    restart)
        if [ -z "$SERVICE_ARG" ]; then
            error "Service name required"
            show_help
            exit 1
        fi
        svc=$(find_service "$SERVICE_ARG") || { error "Unknown service: $SERVICE_ARG"; exit 1; }
        service="${svc%%:*}"
        port="${svc##*:}"
        stop_service "$service"
        start_service "$service" "$port"
        ;;
    start-all)
        start_all
        ;;
    stop-all)
        stop_all
        ;;
    status)
        show_status
        ;;
    logs)
        if [ -z "$SERVICE_ARG" ]; then
            error "Service name required"
            show_help
            exit 1
        fi
        svc=$(find_service "$SERVICE_ARG") || { error "Unknown service: $SERVICE_ARG"; exit 1; }
        service="${svc%%:*}"
        show_logs "$service"
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        error "Unknown command: $COMMAND"
        show_help
        exit 1
        ;;
esac