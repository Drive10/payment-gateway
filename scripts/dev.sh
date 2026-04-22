#!/bin/bash
# =============================================================================
# PayFlow Local Development Script
# Starts infrastructure in Docker, services run locally for fast iteration
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

# Services to start locally
SERVICES=(
    "api-gateway:8080"
    "auth-service:8081"
    "order-service:8082"
    "payment-service:8083"
    "notification-service:8084"
    "simulator-service:8086"
)

stop_services() {
    log "Stopping local services..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true
    log "Local services stopped"
}

start_infra() {
    log "Starting infrastructure (Docker)..."
    docker compose -f docker-compose.dev.yml up -d
    log "Waiting for infra to be healthy..."
    
    # Wait for PostgreSQL
    local max_attempts=30
    local attempt=0
    while ! docker exec postgres pg_isready -U payflow >/dev/null 2>&1; do
        attempt=$((attempt + 1))
        if [ $attempt -gt $max_attempts ]; then
            error "PostgreSQL failed to start"
            exit 1
        fi
        sleep 1
    done
    log "PostgreSQL is ready"
    
    # Wait for Redis
    attempt=0
    while ! docker exec redis redis-cli -a payflow ping >/dev/null 2>&1; do
        attempt=$((attempt + 1))
        if [ $attempt -gt $max_attempts ]; then
            error "Redis failed to start"
            exit 1
        fi
        sleep 1
    done
    log "Redis is ready"
    
    # Wait for Kafka
    attempt=0
    while ! docker exec kafka kafka-broker-start.sh /etc/confluent/docker/config >/dev/null 2>&1; do
        attempt=$((attempt + 1))
        if [ $attempt -gt $max_attempts ]; then
            warn "Kafka startup check skipped"
            break
        fi
        sleep 1
    done
    log "Kafka is ready"
    
    log "All infrastructure ready!"
}

start_services() {
    log "Building services..."
    mvn clean package -DskipTests -q
    
    log "Starting services locally..."
    
    for svc in "${SERVICES[@]}"; do
        service="${svc%%:*}"
        port="${svc##*:}"
        
        info "Starting $service on port $port..."
        nohup mvn spring-boot:run \
            -pl src/$service \
            -Dspring-boot.run.profiles=local \
            -Dspring-boot.run.arguments="--spring.config.additional-location=file:./src/$service/src/main/resources/application-local.yml" \
            > /tmp/$service.log 2>&1 &
    done
    
    log "All services started (check logs in /tmp/<service>.log)"
}

start_frontend() {
    if [ -d "frontend/payment-page" ]; then
        log "Starting frontend..."
        cd frontend/payment-page
        nohup npm run dev > /tmp/frontend.log 2>&1 &
        cd "$SCRIPT_DIR"
    fi
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
                warn "$service not ready after ${timeout}s"
                break
            fi
            sleep 2
            count=$((count + 2))
        done
        log "$service is ready"
    done
}

show_status() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}  PayFlow Local Development Ready!${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "  ${GREEN}Frontend:${NC}    http://localhost:5173"
    echo -e "  ${GREEN}API Gateway:${NC} http://localhost:8080"
    echo -e "  ${GREEN}Auth Service:${NC} http://localhost:8081"
    echo -e "  ${GREEN}Order Service:${NC} http://localhost:8082"
    echo -e "  ${GREEN}Payment:${NC}     http://localhost:8083"
    echo -e "  ${GREEN}Notification:${NC} http://localhost:8084"
    echo -e "  ${GREEN}Simulator:${NC}  http://localhost:8086"
    echo ""
    info "View logs: tail -f /tmp/<service>.log"
    info "Stop services: make local-stop"
}

case "${1:-start}" in
    start)
        start_infra
        start_services
        start_frontend
        wait_for_services
        show_status
        ;;
    stop)
        stop_services
        log "Stopping Docker infra..."
        docker compose -f docker-compose.dev.yml down
        ;;
    restart)
        stop_services
        sleep 2
        start_services
        start_frontend
        wait_for_services
        show_status
        ;;
    logs)
        if [ -z "$2" ]; then
            tail -f /tmp/api-gateway.log
        else
            tail -f "/tmp/$2.log" 2>/dev/null || echo "Log not found: /tmp/$2.log"
        fi
        ;;
    status)
        echo "Docker Infrastructure:"
        docker compose -f docker-compose.dev.yml ps
        echo ""
        echo "Local Services:"
        for svc in "${SERVICES[@]}"; do
            service="${svc%%:*}"
            port="${svc##*:}"
            if curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; then
                echo -e "${GREEN}✓${NC} $service (port $port)"
            else
                echo -e "${RED}✗${NC} $service (port $port)"
            fi
        done
        ;;
    infra)
        start_infra
        ;;
    help|--help|-h)
        echo "PayFlow Local Development"
        echo ""
        echo "Usage: $0 {start|stop|restart|logs|status|infra}"
        echo ""
        echo "Commands:"
        echo "  start     - Start all (infra + services + frontend)"
        echo "  stop      - Stop everything"
        echo "  restart  - Restart services"
        echo "  logs     - View logs (optional: service name)"
        echo "  status   - Check status"
        echo "  infra    - Start infrastructure only"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use: $0 help"
        exit 1
        ;;
esac