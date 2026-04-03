#!/usr/bin/env bash
# Quick API testing shortcuts
# Usage: ./dev/test.sh <command>
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

GATEWAY="http://localhost:8080/api/v1"
TOKEN_FILE="/tmp/payflow_token.txt"

get_token() {
    if [ -f "$TOKEN_FILE" ]; then
        cat "$TOKEN_FILE"
    else
        echo -e "${RED}No token found. Run: $0 login${NC}"
        exit 1
    fi
}

pretty_json() {
    if command -v jq >/dev/null 2>&1; then
        jq .
    else
        cat
    fi
}

case "${1:-help}" in
    login)
        echo -e "${CYAN}Logging in as admin...${NC}"
        RESP=$(curl -s -X POST "$GATEWAY/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"email":"admin@payflow.com","password":"Test@1234"}')
        echo "$RESP" | pretty_json
        TOKEN=$(echo "$RESP" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        if [ -n "$TOKEN" ]; then
            echo "$TOKEN" > "$TOKEN_FILE"
            echo -e "${GREEN}✓ Token saved${NC}"
        else
            echo -e "${RED}Login failed${NC}"
        fi
        ;;

    register)
        EMAIL="${2:-demo@payflow.com}"
        echo -e "${CYAN}Registering $EMAIL...${NC}"
        curl -s -X POST "$GATEWAY/auth/register" \
            -H "Content-Type: application/json" \
            -d "{\"email\":\"$EMAIL\",\"password\":\"Test@1234\",\"firstName\":\"Demo\",\"lastName\":\"User\",\"role\":\"USER\"}" | pretty_json
        ;;

    order)
        TOKEN=$(get_token)
        AMOUNT="${2:-1000}"
        echo -e "${CYAN}Creating order ($AMOUNT)...${NC}"
        curl -s -X POST "$GATEWAY/orders" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "{\"amount\":$AMOUNT,\"currency\":\"USD\",\"items\":[{\"name\":\"Test Item\",\"quantity\":1,\"price\":$AMOUNT}]}" | pretty_json
        ;;

    payment)
        TOKEN=$(get_token)
        ORDER_ID="${2:?Usage: $0 payment <order-id>}"
        AMOUNT="${3:-1000}"
        IDEMP_KEY=$(uuidgen 2>/dev/null || cat /dev/urandom | tr -dc 'a-f0-9' | head -c 36)
        echo -e "${CYAN}Creating payment for order $ORDER_ID...${NC}"
        curl -s -X POST "$GATEWAY/payments" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Idempotency-Key: $IDEMP_KEY" \
            -d "{\"orderId\":\"$ORDER_ID\",\"amount\":$AMOUNT,\"currency\":\"USD\",\"provider\":\"razorpay\",\"method\":\"CARD\",\"merchantId\":\"550e8400-e29b-41d4-a716-446655440000\"}" | pretty_json
        ;;

    capture)
        TOKEN=$(get_token)
        PAYMENT_ID="${2:?Usage: $0 capture <payment-id>}"
        AMOUNT="${3:-1000}"
        IDEMP_KEY=$(uuidgen 2>/dev/null || cat /dev/urandom | tr -dc 'a-f0-9' | head -c 36)
        echo -e "${CYAN}Capturing payment $PAYMENT_ID...${NC}"
        curl -s -X POST "$GATEWAY/payments/$PAYMENT_ID/capture" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Idempotency-Key: $IDEMP_KEY" \
            -d "{\"amount\":$AMOUNT}" | pretty_json
        ;;

    health)
        echo -e "${CYAN}Checking service health...${NC}"
        echo ""
        for port in 8081 8082 8083 8084 8085 8086 8087 8088 8089 8090 8091; do
            name=$(case $port in
                8081) echo "auth-service" ;;
                8082) echo "order-service" ;;
                8083) echo "payment-service" ;;
                8084) echo "notification-service" ;;
                8085) echo "webhook-service" ;;
                8086) echo "simulator-service" ;;
                8087) echo "settlement-service" ;;
                8088) echo "risk-service" ;;
                8089) echo "analytics-service" ;;
                8090) echo "merchant-service" ;;
                8091) echo "dispute-service" ;;
            esac)
            status=$(curl -s --connect-timeout 2 "http://localhost:$port/actuator/health" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
            if [ "$status" = "UP" ]; then
                echo -e "  ${GREEN}●${NC} $name ($port)"
            else
                echo -e "  ${RED}○${NC} $name ($port)"
            fi
        done
        ;;

    e2e)
        echo -e "${CYAN}Running full E2E flow...${NC}"
        echo ""
        
        echo -e "${YELLOW}1. Register...${NC}"
        curl -s -X POST "$GATEWAY/auth/register" \
            -H "Content-Type: application/json" \
            -d '{"email":"e2e@test.com","password":"Test@1234","firstName":"E2E","lastName":"Test","role":"USER"}' | pretty_json
        echo ""

        echo -e "${YELLOW}2. Login...${NC}"
        RESP=$(curl -s -X POST "$GATEWAY/auth/login" \
            -H "Content-Type: application/json" \
            -d '{"email":"e2e@test.com","password":"Test@1234"}')
        echo "$RESP" | pretty_json
        TOKEN=$(echo "$RESP" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
        echo "$TOKEN" > "$TOKEN_FILE"
        echo ""

        echo -e "${YELLOW}3. Create Order...${NC}"
        ORDER=$(curl -s -X POST "$GATEWAY/orders" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d '{"amount":3000,"currency":"USD","items":[{"name":"E2E Test","quantity":1,"price":3000}]}')
        echo "$ORDER" | pretty_json
        ORDER_ID=$(echo "$ORDER" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo ""

        echo -e "${YELLOW}4. Create Payment...${NC}"
        IDEMP_KEY=$(uuidgen 2>/dev/null || cat /dev/urandom | tr -dc 'a-f0-9' | head -c 36)
        PAYMENT=$(curl -s -X POST "$GATEWAY/payments" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Idempotency-Key: $IDEMP_KEY" \
            -d "{\"orderId\":\"$ORDER_ID\",\"amount\":3000,\"currency\":\"USD\",\"provider\":\"razorpay\",\"method\":\"CARD\",\"merchantId\":\"550e8400-e29b-41d4-a716-446655440000\"}")
        echo "$PAYMENT" | pretty_json
        PAYMENT_ID=$(echo "$PAYMENT" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo ""

        echo -e "${YELLOW}5. Capture Payment...${NC}"
        IDEMP_KEY2=$(uuidgen 2>/dev/null || cat /dev/urandom | tr -dc 'a-f0-9' | head -c 36)
        curl -s -X POST "$GATEWAY/payments/$PAYMENT_ID/capture" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Idempotency-Key: $IDEMP_KEY2" \
            -d '{"amount":3000}' | pretty_json
        echo ""
        echo -e "${GREEN}✓ E2E flow complete!${NC}"
        ;;

    help|*)
        echo -e "${CYAN}API Test Shortcuts${NC}"
        echo ""
        echo -e "${YELLOW}Auth:${NC}"
        echo "  $0 login          Login as admin, save token"
        echo "  $0 register [email]  Register new user"
        echo ""
        echo -e "${YELLOW}Orders & Payments:${NC}"
        echo "  $0 order [amount]     Create order"
        echo "  $0 payment <order-id> Create payment"
        echo "  $0 capture <pay-id>   Capture payment"
        echo ""
        echo -e "${YELLOW}Testing:${NC}"
        echo "  $0 health         Check all service health"
        echo "  $0 e2e            Run full E2E flow"
        ;;
esac
