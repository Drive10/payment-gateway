#!/usr/bin/env bash
# Multi-service log viewer with color coding
# Usage: ./dev/tail.sh [service1] [service2] ...
# Example: ./dev/tail.sh payment-service auth-service
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

SERVICES=("$@")

if [ ${#SERVICES[@]} -eq 0 ]; then
    echo -e "${YELLOW}Usage: $0 <service1> [service2] ...${NC}"
    echo ""
    echo -e "${CYAN}Available services:${NC}"
    echo "  api-gateway, auth-service, order-service, payment-service"
    echo "  notification-service, webhook-service, simulator-service"
    echo "  settlement-service, risk-service, analytics-service"
    echo "  merchant-service, dispute-service"
    echo ""
    echo -e "${CYAN}Examples:${NC}"
    echo "  $0 payment-service"
    echo "  $0 payment-service order-service auth-service"
    echo "  $0 api-gateway payment-service"
    exit 1
fi

# Check if services are running in Docker
RUNNING_SERVICES=()
for svc in "${SERVICES[@]}"; do
    if docker compose -f docker-compose.dev.yml --env-file .env.example ps --format json 2>/dev/null | grep -q "\"$svc\""; then
        RUNNING_SERVICES+=("$svc")
    fi
done

if [ ${#RUNNING_SERVICES[@]} -eq 0 ]; then
    echo -e "${YELLOW}None of the specified services are running in Docker.${NC}"
    echo -e "${YELLOW}Falling back to local service logs (target/ directory)...${NC}"
    
    for svc in "${SERVICES[@]}"; do
        log_file="services/$svc/target/spring-boot.log"
        if [ -f "$log_file" ]; then
            echo -e "${GREEN}=== $svc ===${NC}"
            tail -f "$log_file" &
        else
            echo -e "${RED}No log file found for $svc${NC}"
        fi
    done
    wait
    exit 0
fi

echo -e "${CYAN}Tailing logs for: ${GREEN}${RUNNING_SERVICES[*]}${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo ""

# Color codes for each service
COLORS=('\033[36m' '\033[32m' '\033[33m' '\033[35m' '\033[34m')

for i in "${!RUNNING_SERVICES[@]}"; do
    svc="${RUNNING_SERVICES[$i]}"
    color="${COLORS[$((i % ${#COLORS[@]}))]}"
    
    docker compose -f docker-compose.dev.yml --env-file .env.example logs -f --tail=50 "$svc" 2>&1 | \
        while IFS= read -r line; do
            echo -e "${color}[$svc]${NC} $line"
        done &
done

wait
