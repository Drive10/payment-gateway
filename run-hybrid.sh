#!/bin/bash
# Payment Gateway - Hybrid Mode Testing
# Usage: ./run-hybrid.sh <scenario>
# Scenarios: infra-only, mixed, network-partition, service-restart, db-failover, kafka-lag, all

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_header() {
    echo ""
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    echo ""
}

log_info() { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1"; }
log_success() { echo -e "${GREEN}[$(date '+%H:%M:%S')]${NC} $1"; }
log_error() { echo -e "${RED}[$(date '+%H:%M:%S')]${NC} $1"; }

wait_for_health() {
    local url=$1
    local timeout=${2:-60}
    local elapsed=0
    
    while [[ $elapsed -lt $timeout ]]; do
        if curl -s -f "$url" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
    done
    return 1
}

wait_for_user() {
    echo -e "${YELLOW}$1${NC}"
    read -r
}

# Scenario 1: Infrastructure Only
scenario_infra_only() {
    log_header "Scenario: Infrastructure Only (for local development)"
    
    log_info "Starting infrastructure services..."
    docker compose --profile infra up -d
    
    log_info "Waiting for infrastructure..."
    sleep 15
    
    echo ""
    echo -e "${YELLOW}Infrastructure is running. Run services locally:${NC}"
    echo "  ./run-dev.sh --service auth"
    echo "  ./run-dev.sh --service payment"
    echo "  ./run-dev.sh --service gateway"
    echo ""
    echo -e "${YELLOW}Endpoints:${NC}"
    echo "  PostgreSQL: localhost:5433"
    echo "  Redis: localhost:6379"
    echo "  Kafka: localhost:9092"
}

# Scenario 2: Mixed Docker/Local Services
scenario_mixed() {
    log_header "Scenario: Mixed Docker and Local Services"
    
    log_info "Starting infrastructure in Docker..."
    docker compose --profile infra up -d
    sleep 15
    
    log_info "Starting some services in Docker..."
    docker compose up -d auth-service notification-service webhook-service
    sleep 30
    
    echo ""
    echo -e "${GREEN}Docker services running:${NC}"
    echo "  - Auth Service (http://localhost:8081)"
    echo "  - Notification Service (http://localhost:8084)"
    echo "  - Webhook Service (http://localhost:8085)"
    
    echo ""
    echo -e "${YELLOW}Run remaining services locally:${NC}"
    echo "  Terminal 1: cd services/payment-service && mvn spring-boot:run -Dspring-boot.run.profiles=local"
    echo "  Terminal 2: cd services/order-service && mvn spring-boot:run -Dspring-boot.run.profiles=local"
    echo "  Terminal 3: cd services/api-gateway && mvn spring-boot:run -Dspring-boot.run.profiles=local"
    
    echo ""
    echo -e "${YELLOW}Note: Docker services use container names (e.g., http://auth-service:8081)${NC}"
    echo -e "${YELLOW}      Local services use localhost${NC}"
}

# Scenario 3: Network Partition Simulation
scenario_network_partition() {
    log_header "Scenario: Network Partition Simulation"
    
    log_info "Starting all services..."
    docker compose --profile services up -d
    sleep 90
    
    log_info "Checking initial health..."
    docker compose ps
    
    log_info "Simulating network partition - disconnecting payment-service..."
    docker network disconnect fintech-network payment-service 2>/dev/null || log_error "Could not disconnect"
    
    echo ""
    echo -e "${YELLOW}Payment service is now isolated. Test these scenarios:${NC}"
    echo "  1. Try to create an order (should fail gracefully)"
    echo "  2. Check order-service logs for retry attempts"
    echo "  3. Check API Gateway error handling"
    
    wait_for_user "Press Enter to reconnect payment-service..."
    
    log_info "Reconnecting payment-service..."
    docker network connect fintech-network payment-service 2>/dev/null || log_error "Could not reconnect"
    
    log_info "Waiting for recovery..."
    sleep 30
    
    log_info "Checking health after recovery..."
    docker compose ps
}

# Scenario 4: Service Restart and Recovery
scenario_service_restart() {
    log_header "Scenario: Service Restart and Recovery"
    
    log_info "Starting all services..."
    docker compose --profile services up -d
    sleep 90
    
    log_info "Killing payment-service..."
    docker compose kill payment-service
    
    echo ""
    echo -e "${YELLOW}Payment service is down. Test these scenarios:${NC}"
    echo "  1. Try to access payment endpoints via API Gateway"
    echo "  2. Check order-service logs for retry behavior"
    echo "  3. Check circuit breaker activation"
    
    wait_for_user "Press Enter to restart payment-service..."
    
    log_info "Restarting payment-service..."
    docker compose start payment-service
    
    log_info "Waiting for service to be healthy..."
    if wait_for_health "http://localhost:8083/actuator/health" 60; then
        log_success "Payment service recovered!"
    else
        log_error "Payment service failed to recover"
    fi
    
    docker compose ps
}

# Scenario 5: Database Failover
scenario_db_failover() {
    log_header "Scenario: Database Failover"
    
    log_info "Starting infrastructure..."
    docker compose --profile infra up -d
    sleep 15
    
    log_info "Starting services..."
    docker compose up -d auth-service payment-service
    sleep 45
    
    log_info "Current status:"
    docker compose ps
    
    log_info "Stopping PostgreSQL..."
    docker compose stop postgres
    
    echo ""
    echo -e "${YELLOW}PostgreSQL is down. Test these scenarios:${NC}"
    echo "  1. Check service health endpoints"
    echo "  2. Try to make API calls (should fail gracefully)"
    echo "  3. Check service logs for connection errors"
    
    wait_for_user "Press Enter to restart PostgreSQL..."
    
    log_info "Restarting PostgreSQL..."
    docker compose start postgres
    
    log_info "Waiting for PostgreSQL to be ready..."
    sleep 20
    
    log_info "Checking service recovery..."
    docker compose ps
}

# Scenario 6: Kafka Consumer Lag
scenario_kafka_lag() {
    log_header "Scenario: Kafka Consumer Lag"
    
    log_info "Starting infrastructure and producers..."
    docker compose --profile infra up -d
    sleep 15
    
    docker compose up -d auth-service payment-service
    sleep 45
    
    echo ""
    echo -e "${YELLOW}Services are producing events. Now start consumer:${NC}"
    echo "  docker compose up -d notification-service"
    echo ""
    echo -e "${YELLOW}Check consumer lag with:${NC}"
    echo "  docker compose exec kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group notification-group"
    
    wait_for_user "Press Enter to start notification-service..."
    
    log_info "Starting notification-service..."
    docker compose up -d notification-service
    sleep 30
    
    log_info "Checking notification logs..."
    docker compose logs notification-service --tail 50
}

# Clean up
cleanup() {
    log_info "Cleaning up all Docker resources..."
    docker compose --profile services --profile infra --profile observability down -v
    docker system prune -f 2>/dev/null || true
    log_success "Cleanup complete!"
}

# Show help
show_help() {
    echo -e "${YELLOW}Payment Gateway - Hybrid Mode Testing${NC}"
    echo ""
    echo "Available Scenarios:"
    echo "  infra-only         - Start only infrastructure for local development"
    echo "  mixed              - Mix of Docker and local services"
    echo "  network-partition  - Simulate network partition between services"
    echo "  service-restart    - Test service restart and recovery"
    echo "  db-failover        - Test database failover behavior"
    echo "  kafka-lag          - Test Kafka consumer lag scenarios"
    echo "  all                - Run all scenarios sequentially"
    echo "  cleanup            - Clean up all Docker resources"
    echo ""
    echo "Usage:"
    echo "  $0 <scenario>"
    echo "  $0 cleanup"
}

# Main
case "${1:-}" in
    infra-only)       scenario_infra_only ;;
    mixed)            scenario_mixed ;;
    network-partition) scenario_network_partition ;;
    service-restart)  scenario_service_restart ;;
    db-failover)      scenario_db_failover ;;
    kafka-lag)        scenario_kafka_lag ;;
    all)
        scenario_infra_only
        cleanup
        scenario_mixed
        cleanup
        scenario_network_partition
        cleanup
        scenario_service_restart
        cleanup
        scenario_db_failover
        cleanup
        scenario_kafka_lag
        cleanup
        log_success "All scenarios completed!"
        ;;
    cleanup)          cleanup ;;
    help|--help|-h)   show_help ;;
    *)
        show_help
        exit 1
        ;;
esac
