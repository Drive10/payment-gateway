#!/usr/bin/env bash
# Hybrid Local Dev - Quick Start
# Usage: ./dev/start.sh
#
# This starts infrastructure in Docker and shows you how to run services locally.

set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Payment Gateway - Hybrid Dev Mode         ║${NC}"
echo -e "${CYAN}║   Infra: Docker | Services: localhost       ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════╝${NC}"
echo ""

# Check dependencies
echo -e "${YELLOW}Checking dependencies...${NC}"

command -v docker >/dev/null 2>&1 || { echo -e "${RED}Docker is required${NC}"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo -e "${RED}Maven is required${NC}"; exit 1; }
command -v java >/dev/null 2>&1 || { echo -e "${RED}Java is required${NC}"; exit 1; }

echo -e "${GREEN}✓ All dependencies found${NC}"
echo ""

# Start infrastructure
echo -e "${CYAN}Starting infrastructure (Postgres, Redis, Kafka, Gateway, Frontends)...${NC}"
docker compose -f docker-compose.dev.yml --env-file .env.dev up -d

echo ""
echo -e "${GREEN}✓ Infrastructure started!${NC}"
echo ""

# Wait for infra
echo -e "${YELLOW}Waiting for infrastructure to be ready...${NC}"
sleep 5

# Check readiness
echo -e "${CYAN}Checking readiness...${NC}"

if curl -s --connect-timeout 3 http://localhost:8080/actuator/health | grep -q "UP"; then
    echo -e "${GREEN}✓ API Gateway is ready (port 8080)${NC}"
else
    echo -e "${YELLOW}⚠ API Gateway still starting...${NC}"
fi

if curl -s --connect-timeout 3 http://localhost:3000/ >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Frontend is ready (port 3000)${NC}"
fi

if curl -s --connect-timeout 3 http://localhost:3001/ >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Dashboard is ready (port 3001)${NC}"
fi

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Infrastructure URLs:                                    ║${NC}"
echo -e "${CYAN}║    Frontend:   http://localhost:3000                     ║${NC}"
echo -e "${CYAN}║    Dashboard:  http://localhost:3001                     ║${NC}"
echo -e "${CYAN}║    API Gateway: http://localhost:8080                    ║${NC}"
echo -e "${CYAN}║    Postgres:   localhost:5433                            ║${NC}"
echo -e "${CYAN}║    Redis:      localhost:6379                            ║${NC}"
echo -e "${CYAN}║    Kafka:      localhost:9092                            ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Run services locally:                                   ║${NC}"
echo -e "${CYAN}║    make dev:run SERVICE=auth-service                     ║${NC}"
echo -e "${CYAN}║    make dev:run SERVICE=payment-service                  ║${NC}"
echo -e "${CYAN}║    make dev:run SERVICE=order-service                    ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Helpful commands:                                       ║${NC}"
echo -e "${CYAN}║    make dev:status          Show all service status      ║${NC}"
echo -e "${CYAN}║    make dev:build SERVICE=x Build a service              ║${NC}"
echo -e "${CYAN}║    make dev:logs SERVICE=x  Follow Docker logs           ║${NC}"
echo -e "${CYAN}║    make dev:db DB=authdb    Open psql                    ║${NC}"
echo -e "${CYAN}║    make dev:redis           Open redis-cli               ║${NC}"
echo -e "${CYAN}║    make dev:stop            Stop everything               ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════╝${NC}"
