#!/bin/bash
# ===========================================
# PayFlow Diagnostics Script
# ===========================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  PayFlow Diagnostics${NC}"
echo -e "${BLUE}===========================================${NC}"
echo ""

# Check Docker
echo -e "${YELLOW}Docker Status:${NC}"
if docker info > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker is running${NC}"
else
    echo -e "${RED}✗ Docker is not running${NC}"
    exit 1
fi

# Check containers
echo ""
echo -e "${YELLOW}Container Status:${NC}"
docker compose ps 2>/dev/null || echo "No containers running"

# Check infrastructure
echo ""
echo -e "${YELLOW}Infrastructure Health:${NC}"
for svc in postgres mongodb redis kafka zookeeper; do
    status=$(docker compose ps --format json $svc 2>/dev/null | grep -o '"health_status":"[^"]*"' | cut -d'"' -f4 || echo "unknown")
    if [ "$status" = "healthy" ]; then
        echo -e "  ${GREEN}✓${NC} $svc: healthy"
    elif [ "$status" = "starting" ] || [ "$status" = "created" ]; then
        echo -e "  ${YELLOW}◐${NC} $svc: starting"
    else
        echo -e "  ${YELLOW}○${NC} $svc: not running (use 'make infra-up' to start)"
    fi
done

# Check local services
echo ""
echo -e "${YELLOW}Local Services Health:${NC}"
for svc in "8080:api-gateway" "8081:auth" "8082:order" "8083:payment" "8084:notification" "8086:simulator" "8089:analytics" "8090:audit"; do
    port="${svc%%:*}"
    name="${svc##*:}"
    if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        echo -e "  ${GREEN}✓${NC} $name (port $port): healthy"
    else
        echo -e "  ${YELLOW}○${NC} $name (port $port): not running"
    fi
done

echo ""
echo -e "${BLUE}===========================================${NC}"
echo -e "${BLUE}  Useful URLs${NC}"
echo -e "${BLUE}===========================================${NC}"
echo -e "  API Gateway:     ${GREEN}http://localhost:8080${NC}"
echo -e "  Auth Service:    ${GREEN}http://localhost:8081${NC}"
echo -e "  Payment Service: ${GREEN}http://localhost:8083${NC}"
echo -e "  Zipkin:          ${GREEN}http://localhost:9411${NC}"
echo ""
