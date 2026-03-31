#!/bin/bash
# Payment Gateway - Docker Full Run
# Usage: ./run-docker.sh [--up|--down|--status|--logs] [--observability] [--service <name>]

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

PROFILES=()

start_all() {
    local profiles=("--profile" "services")
    
    if [[ "$1" == "--observability" ]]; then
        profiles+=("--profile" "observability")
    fi
    
    log_info "Building all service images..."
    docker compose build
    
    log_info "Starting all services..."
    docker compose "${profiles[@]}" up -d
    
    log_info "Waiting for services to be healthy (2-3 minutes)..."
    sleep 90
    
    log_success "All services started!"
    echo ""
    echo -e "${YELLOW}Service Endpoints:${NC}"
    echo "  - API Gateway:    http://localhost:8080"
    echo "  - Auth Service:   http://localhost:8081"
    echo "  - Order Service:  http://localhost:8082"
    echo "  - Payment Svc:    http://localhost:8083"
    echo "  - Notification:   http://localhost:8084"
    echo "  - Webhook:        http://localhost:8085"
    echo "  - Simulator:      http://localhost:8086"
    echo "  - Settlement:     http://localhost:8087"
    echo "  - Risk:           http://localhost:8088"
    echo "  - Analytics:      http://localhost:8089"
    echo "  - Merchant:       http://localhost:8090"
    echo "  - Dispute:        http://localhost:8091"
    
    if [[ "$1" == "--observability" ]]; then
        echo ""
        echo -e "${YELLOW}Observability Stack:${NC}"
        echo "  - Grafana:    http://localhost:3002 (admin/admin)"
        echo "  - Prometheus: http://localhost:9090"
        echo "  - Jaeger:     http://localhost:16686"
    fi
}

stop_all() {
    log_info "Stopping all services..."
    docker compose --profile services --profile infra --profile observability down -v
    log_success "All services stopped!"
}

show_status() {
    docker compose ps
    echo ""
    log_info "Checking service health..."
    
    local services=(
        "api-gateway:8080"
        "auth-service:8081"
        "order-service:8082"
        "payment-service:8083"
        "notification-service:8084"
        "webhook-service:8085"
        "simulator-service:8086"
        "settlement-service:8087"
        "risk-service:8088"
        "analytics-service:8089"
        "merchant-service:8090"
        "dispute-service:8091"
    )
    
    for svc in "${services[@]}"; do
        IFS=':' read -r name port <<< "$svc"
        if curl -s -f "http://localhost:${port}/actuator/health" >/dev/null 2>&1; then
            echo -e "  ${GREEN}[UP]${NC}   $name - http://localhost:$port"
        else
            echo -e "  ${RED}[DOWN]${NC} $name - http://localhost:$port"
        fi
    done
}

show_logs() {
    if [[ -n "$1" ]]; then
        docker compose logs -f "$1"
    else
        docker compose logs -f
    fi
}

show_help() {
    echo -e "${YELLOW}Payment Gateway - Docker Full Run${NC}"
    echo ""
    echo "Usage:"
    echo "  $0 --up                        Start all services"
    echo "  $0 --up --observability        Start with Grafana/Prometheus/Jaeger"
    echo "  $0 --down                      Stop all services"
    echo "  $0 --status                    Show service status"
    echo "  $0 --logs                      Show all logs"
    echo "  $0 --logs --service <name>     Show logs for specific service"
    echo "  $0 --help                      Show this help"
}

# Parse arguments
OBSERVABILITY=""
SERVICE=""
ACTION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --up)             ACTION="up"; shift ;;
        --down)           ACTION="down"; shift ;;
        --status)         ACTION="status"; shift ;;
        --logs)           ACTION="logs"; shift ;;
        --observability)  OBSERVABILITY="--observability"; shift ;;
        --service)        SERVICE="$2"; shift 2 ;;
        --help|-h)        show_help; exit 0 ;;
        *)                log_error "Unknown option: $1"; show_help; exit 1 ;;
    esac
done

case "$ACTION" in
    up)     start_all "$OBSERVABILITY" ;;
    down)   stop_all ;;
    status) show_status ;;
    logs)   show_logs "$SERVICE" ;;
    *)      show_help ;;
esac
