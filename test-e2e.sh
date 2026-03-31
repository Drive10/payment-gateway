#!/bin/bash
# Payment Gateway - E2E API Testing
# Usage: ./test-e2e.sh [--health|--auth|--orders|--payments|--all] [--url <base-url>]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

BASE_URL="${BASE_URL:-http://localhost:8080}"
AUTH_TOKEN=""

log_test() { echo -e "${CYAN}[TEST]${NC} $1"; }
log_pass() { echo -e "  ${GREEN}[PASS]${NC} $1"; }
log_fail() { echo -e "  ${RED}[FAIL]${NC} $1"; }

test_health() {
    echo ""
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    echo -e "${YELLOW}Testing Health Endpoints${NC}"
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    
    local endpoints=(
        "API Gateway:8080"
        "Auth Service:8081"
        "Order Service:8082"
        "Payment Service:8083"
        "Notification Service:8084"
        "Webhook Service:8085"
        "Simulator Service:8086"
        "Settlement Service:8087"
        "Risk Service:8088"
        "Analytics Service:8089"
        "Merchant Service:8090"
        "Dispute Service:8091"
    )
    
    for ep in "${endpoints[@]}"; do
        IFS=':' read -r name port <<< "$ep"
        log_test "$name Health"
        
        response=$(curl -s -w "%{http_code}" -o /tmp/health_response "http://localhost:${port}/actuator/health" 2>/dev/null || echo "000")
        
        if [[ "$response" == "200" ]]; then
            status=$(cat /tmp/health_response | grep -o '"status":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "UNKNOWN")
            if [[ "$status" == "UP" ]]; then
                log_pass "Service is UP"
            else
                log_fail "Service status: $status"
            fi
        else
            log_fail "Service not reachable (HTTP $response)"
        fi
    done
    rm -f /tmp/health_response
}

test_auth() {
    echo ""
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    echo -e "${YELLOW}Testing Auth Endpoints${NC}"
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    
    local timestamp=$(date +%s)
    local email="test${timestamp}@example.com"
    
    # Register
    log_test "User Registration"
    response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/auth/register" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"${email}\",\"password\":\"Test@123456\",\"fullName\":\"Test User\"}" 2>/dev/null)
    
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -n -1)
    
    if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
        log_pass "User registered: $email"
    else
        log_fail "Registration failed (HTTP $http_code)"
    fi
    
    # Login
    log_test "User Login"
    response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"${email}\",\"password\":\"Test@123456\"}" 2>/dev/null)
    
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -n -1)
    
    if [[ "$http_code" == "200" ]]; then
        AUTH_TOKEN=$(echo "$body" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 2>/dev/null || echo "")
        if [[ -n "$AUTH_TOKEN" ]]; then
            log_pass "Login successful, token obtained"
        else
            log_fail "No access token in response"
        fi
    else
        log_fail "Login failed (HTTP $http_code)"
    fi
    
    # Get Profile
    log_test "Get User Profile"
    if [[ -n "$AUTH_TOKEN" ]]; then
        response=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/auth/profile" \
            -H "Authorization: Bearer ${AUTH_TOKEN}" 2>/dev/null)
        
        http_code=$(echo "$response" | tail -1)
        
        if [[ "$http_code" == "200" ]]; then
            log_pass "Profile retrieved"
        else
            log_fail "Profile fetch failed (HTTP $http_code)"
        fi
    else
        log_fail "No auth token available"
    fi
}

test_orders() {
    echo ""
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    echo -e "${YELLOW}Testing Order Endpoints${NC}"
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    
    if [[ -z "$AUTH_TOKEN" ]]; then
        log_fail "Auth token required. Run --auth first."
        return
    fi
    
    # Create Order
    log_test "Create Order"
    response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/orders" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${AUTH_TOKEN}" \
        -d '{"items":[{"productId":"PROD-001","productName":"Test Product","quantity":1,"unitPrice":99.99}],"currency":"USD"}' 2>/dev/null)
    
    http_code=$(echo "$response" | tail -1)
    body=$(echo "$response" | head -n -1)
    
    if [[ "$http_code" == "200" ]] || [[ "$http_code" == "201" ]]; then
        ORDER_ID=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4 2>/dev/null || echo "")
        log_pass "Order created: $ORDER_ID"
    else
        log_fail "Create order failed (HTTP $http_code)"
    fi
    
    # Get Order
    if [[ -n "$ORDER_ID" ]]; then
        log_test "Get Order"
        response=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/orders/${ORDER_ID}" \
            -H "Authorization: Bearer ${AUTH_TOKEN}" 2>/dev/null)
        
        http_code=$(echo "$response" | tail -1)
        
        if [[ "$http_code" == "200" ]]; then
            log_pass "Order retrieved"
        else
            log_fail "Get order failed (HTTP $http_code)"
        fi
    fi
    
    # List Orders
    log_test "List Orders"
    response=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/orders" \
        -H "Authorization: Bearer ${AUTH_TOKEN}" 2>/dev/null)
    
    http_code=$(echo "$response" | tail -1)
    
    if [[ "$http_code" == "200" ]]; then
        log_pass "Orders listed successfully"
    else
        log_fail "List orders failed (HTTP $http_code)"
    fi
}

test_payments() {
    echo ""
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    echo -e "${YELLOW}Testing Payment Endpoints${NC}"
    echo -e "${YELLOW}$(printf '=%.0s' {1..60})${NC}"
    
    if [[ -z "$AUTH_TOKEN" ]]; then
        log_fail "Auth token required. Run --auth first."
        return
    fi
    
    # List Payments
    log_test "List Payments"
    response=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/payments" \
        -H "Authorization: Bearer ${AUTH_TOKEN}" 2>/dev/null)
    
    http_code=$(echo "$response" | tail -1)
    
    if [[ "$http_code" == "200" ]]; then
        log_pass "Payments listed successfully"
    else
        log_fail "List payments failed (HTTP $http_code)"
    fi
}

show_help() {
    echo -e "${YELLOW}Payment Gateway - E2E Testing${NC}"
    echo ""
    echo "Usage:"
    echo "  $0 --all                    Run all tests"
    echo "  $0 --health                 Test health endpoints"
    echo "  $0 --auth                   Test authentication"
    echo "  $0 --orders                 Test order endpoints"
    echo "  $0 --payments               Test payment endpoints"
    echo "  $0 --url http://staging:8080  Custom base URL"
    echo ""
    echo "Environment variables:"
    echo "  BASE_URL                    Base URL (default: http://localhost:8080)"
}

# Parse arguments
ACTION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --all)      ACTION="all"; shift ;;
        --health)   ACTION="health"; shift ;;
        --auth)     ACTION="auth"; shift ;;
        --orders)   ACTION="orders"; shift ;;
        --payments) ACTION="payments"; shift ;;
        --url)      BASE_URL="$2"; shift 2 ;;
        --help|-h)  show_help; exit 0 ;;
        *)          log_fail "Unknown option: $1"; show_help; exit 1 ;;
    esac
done

case "$ACTION" in
    all)
        test_health
        test_auth
        test_orders
        test_payments
        ;;
    health)   test_health ;;
    auth)     test_auth ;;
    orders)   test_orders ;;
    payments) test_payments ;;
    *)        show_help ;;
esac
