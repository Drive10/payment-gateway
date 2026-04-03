#!/usr/bin/env bash
# Seed demo data for local development
# Usage: ./dev/seed.sh
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

GATEWAY="http://localhost:8080/api/v1"

echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Seeding Demo Data                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

# Check gateway is running
if ! curl -s --connect-timeout 3 "$GATEWAY/actuator/health" | grep -q "UP"; then
    echo -e "${RED}API Gateway not running at $GATEWAY${NC}"
    echo -e "${YELLOW}Start it with: make dev:start${NC}"
    exit 1
fi

# 1. Create admin user
echo -e "${YELLOW}Creating admin user...${NC}"
ADMIN_RESP=$(curl -s -X POST "$GATEWAY/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@payflow.com","password":"Test@1234","firstName":"Admin","lastName":"User","role":"ADMIN"}')
echo -e "${GREEN}✓ Admin user created${NC}"

# 2. Create regular user
echo -e "${YELLOW}Creating regular user...${NC}"
USER_RESP=$(curl -s -X POST "$GATEWAY/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"john@payflow.com","password":"Test@1234","firstName":"John","lastName":"Doe","role":"USER"}')
echo -e "${GREEN}✓ Regular user created${NC}"

# 3. Login as admin and get token
echo -e "${YELLOW}Logging in as admin...${NC}"
TOKEN=$(curl -s -X POST "$GATEWAY/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@payflow.com","password":"Test@1234"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}Failed to get auth token${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Admin token obtained${NC}"

# 4. Create orders
echo -e "${YELLOW}Creating orders...${NC}"
ORDER1=$(curl -s -X POST "$GATEWAY/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":5000,"currency":"USD","items":[{"name":"Premium Plan","quantity":1,"price":5000}]}')
ORDER1_ID=$(echo "$ORDER1" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo -e "${GREEN}✓ Order 1: $ORDER1_ID${NC}"

ORDER2=$(curl -s -X POST "$GATEWAY/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":2500,"currency":"USD","items":[{"name":"Basic Plan","quantity":1,"price":2500}]}')
ORDER2_ID=$(echo "$ORDER2" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo -e "${GREEN}✓ Order 2: $ORDER2_ID${NC}"

ORDER3=$(curl -s -X POST "$GATEWAY/orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"amount":10000,"currency":"USD","items":[{"name":"Enterprise Plan","quantity":1,"price":10000}]}')
ORDER3_ID=$(echo "$ORDER3" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo -e "${GREEN}✓ Order 3: $ORDER3_ID${NC}"

# 5. Create and capture payments
echo -e "${YELLOW}Creating payments...${NC}"

create_payment() {
    local order_id=$1
    local amount=$2
    local idemp_key=$(uuidgen 2>/dev/null || cat /dev/urandom | tr -dc 'a-f0-9' | head -c 36)

    local payment=$(curl -s -X POST "$GATEWAY/payments" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Idempotency-Key: $idemp_key" \
      -d "{\"orderId\":\"$order_id\",\"amount\":$amount,\"currency\":\"USD\",\"provider\":\"razorpay\",\"method\":\"CARD\",\"merchantId\":\"550e8400-e29b-41d4-a716-446655440000\"}")

    local payment_id=$(echo "$payment" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    local status=$(echo "$payment" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "$payment_id"
}

PAY1_ID=$(create_payment "$ORDER1_ID" 5000)
echo -e "${GREEN}✓ Payment 1 created: $PAY1_ID${NC}"

PAY2_ID=$(create_payment "$ORDER2_ID" 2500)
echo -e "${GREEN}✓ Payment 2 created: $PAY2_ID${NC}"

# 6. Capture payment 1
echo -e "${YELLOW}Capturing payment 1...${NC}"
if [ -n "$PAY1_ID" ]; then
    curl -s -X POST "$GATEWAY/payments/$PAY1_ID/capture" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Idempotency-Key: $(uuidgen 2>/dev/null || cat /dev/urandom | tr -dc 'a-f0-9' | head -c 36)" \
      -d '{"amount":5000}' > /dev/null
    echo -e "${GREEN}✓ Payment 1 captured${NC}"
fi

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Demo Data Summary                                   ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Admin:  admin@payflow.com / Test@1234               ║${NC}"
echo -e "${CYAN}║  User:   john@payflow.com / Test@1234                ║${NC}"
echo -e "${CYAN}║                                                      ║${NC}"
echo -e "${CYAN}║  Order 1: Premium Plan   ($50.00) - Payment Captured ║${NC}"
echo -e "${CYAN}║  Order 2: Basic Plan     ($25.00) - Payment Created  ║${NC}"
echo -e "${CYAN}║  Order 3: Enterprise Plan($100.00)- No Payment       ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Dashboard: http://localhost:3001/login              ║${NC}"
echo -e "${CYAN}║  Frontend:  http://localhost:3000                    ║${NC}"
echo -e "${CYAN}║  API:       http://localhost:8080                    ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
