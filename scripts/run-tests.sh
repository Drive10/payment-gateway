#!/bin/bash
set -e

# Payment Gateway - Comprehensive Test Runner
# Usage: ./run-tests.sh [options]
#
# Options:
#   --all           Run all test suites
#   --unit          Run unit tests (JUnit + Mockito)
#   --integration   Run integration tests (Testcontainers)
#   --api           Run API tests (Rest Assured)
#   --contract      Run contract tests (Pact)
#   --e2e           Run E2E tests (Playwright)
#   --load          Run load tests (k6)
#   --health        Run health checks only
#   --ci            Run all tests in CI mode (with retries)
#   --parallel      Run independent test suites in parallel
#   --report        Generate test report
#   --help          Show this help message

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TESTS_DIR="$ROOT_DIR/tests"
REPORT_DIR="$ROOT_DIR/test-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
EXIT_CODE=0

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

print_header() {
    echo -e "\n${BLUE}╔══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  $(printf "%-54s" "$1")║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════╝${NC}\n"
}

print_step() {
    echo -e "${CYAN}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
}

print_failure() {
    echo -e "${RED}✗ $1${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    EXIT_CODE=1
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

mkdir -p "$REPORT_DIR"

run_health_checks() {
    print_header "Health Checks"
    
    local services=("api-gateway:8080" "auth-service:8081" "order-service:8082" "payment-service:8083")
    
    for service in "${services[@]}"; do
        local name="${service%%:*}"
        local port="${service##*:}"
        
        if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            print_success "$name (port $port) is healthy"
        else
            print_failure "$name (port $port) is not responding"
        fi
    done
    
    TOTAL_TESTS=$((TOTAL_TESTS + ${#services[@]}))
}

run_unit_tests() {
    print_header "Unit Tests (JUnit + Mockito)"
    
    local services=("auth-service" "payment-service" "risk-service" "notification-service" "api-gateway")
    
    for service in "${services[@]}"; do
        print_step "Running unit tests for $service..."
        
        if (cd "$ROOT_DIR/services/$service" && mvn test -Dtest="**/*Test" -q --batch-mode 2>&1 | tee "$REPORT_DIR/unit-${service}-${TIMESTAMP}.log"); then
            print_success "$service unit tests passed"
        else
            print_failure "$service unit tests failed"
        fi
        
        TOTAL_TESTS=$((TOTAL_TESTS + 1))
    done
}

run_integration_tests() {
    print_header "Integration Tests (Testcontainers)"
    
    print_step "Running integration tests..."
    
    if (cd "$TESTS_DIR/integration" && mvn test -q --batch-mode 2>&1 | tee "$REPORT_DIR/integration-${TIMESTAMP}.log"); then
        print_success "Integration tests passed"
    else
        print_failure "Integration tests failed"
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

run_api_tests() {
    print_header "API Tests (Rest Assured)"
    
    local base_url="${API_BASE_URL:-http://localhost:8080}"
    print_step "Running API tests against $base_url..."
    
    if (cd "$TESTS_DIR/api" && mvn test -Dapi.base.url="$base_url" -q --batch-mode 2>&1 | tee "$REPORT_DIR/api-${TIMESTAMP}.log"); then
        print_success "API tests passed"
    else
        print_failure "API tests failed"
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

run_contract_tests() {
    print_header "Contract Tests (Pact)"
    
    print_step "Running Pact contract tests..."
    
    if (cd "$TESTS_DIR/contracts" && mvn test -q --batch-mode 2>&1 | tee "$REPORT_DIR/contracts-${TIMESTAMP}.log"); then
        print_success "Contract tests passed"
    else
        print_failure "Contract tests failed"
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

run_e2e_tests() {
    print_header "E2E Tests (Playwright)"
    
    print_step "Running Playwright E2E tests..."
    
    if (cd "$TESTS_DIR/e2e" && npx playwright test --reporter=html, junit 2>&1 | tee "$REPORT_DIR/e2e-${TIMESTAMP}.log"); then
        print_success "E2E tests passed"
    else
        print_failure "E2E tests failed"
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

run_load_tests() {
    print_header "Load Tests (k6)"
    
    local vus="${LOAD_VUS:-50}"
    local duration="${LOAD_DURATION:-60}"
    
    if ! command -v k6 &> /dev/null; then
        print_info "k6 not installed. Installing..."
        if [[ "$OSTYPE" == "darwin"* ]]; then
            brew install k6
        else
            print_failure "Please install k6 manually"
            return
        fi
    fi
    
    print_step "Running load test ($vus VUs, ${duration}s)..."
    
    k6 run "$TESTS_DIR/load/scenarios/full-flow.js" \
        --env BASE_URL="${API_BASE_URL:-http://localhost:8080}" \
        --out json="$REPORT_DIR/load-${TIMESTAMP}.json" 2>&1 | tee "$REPORT_DIR/load-${TIMESTAMP}.log"
    
    if [ ${PIPESTATUS[0]} -eq 0 ]; then
        print_success "Load tests completed"
    else
        print_failure "Load tests failed (thresholds not met)"
    fi
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

run_all() {
    run_health_checks
    run_unit_tests
    run_integration_tests
    run_api_tests
    run_contract_tests
    run_e2e_tests
    run_load_tests
}

run_parallel() {
    print_header "Running Tests in Parallel"
    
    run_health_checks &
    local health_pid=$!
    
    run_unit_tests &
    local unit_pid=$!
    
    run_integration_tests &
    local integration_pid=$!
    
    # API, E2E, and Load tests depend on services being up
    wait $health_pid
    
    run_api_tests &
    local api_pid=$!
    
    wait $unit_pid $integration_pid $api_pid
    
    run_e2e_tests &
    local e2e_pid=$!
    
    run_load_tests &
    local load_pid=$!
    
    wait $e2e_pid $load_pid
}

generate_report() {
    print_header "Test Report"
    
    local report_file="$REPORT_DIR/summary-${TIMESTAMP}.md"
    
    cat > "$report_file" << EOF
# Test Execution Summary

**Date:** $(date)
**Total Test Suites:** $TOTAL_TESTS
**Passed:** $PASSED_TESTS
**Failed:** $FAILED_TESTS
**Exit Code:** $EXIT_CODE

## Test Results

| Test Suite | Status |
|------------|--------|
| Health Checks | $([ $HEALTH_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |
| Unit Tests | $([ $UNIT_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |
| Integration Tests | $([ $INTEGRATION_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |
| API Tests | $([ $API_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |
| Contract Tests | $([ $CONTRACT_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |
| E2E Tests | $([ $E2E_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |
| Load Tests | $([ $LOAD_RESULT -eq 0 ] && echo "✅ Passed" || echo "❌ Failed") |

## Log Files

All test logs are available in \`$REPORT_DIR/\`
EOF
    
    echo -e "\n${CYAN}Report generated: $report_file${NC}"
}

show_help() {
    echo "Payment Gateway Test Runner"
    echo ""
    echo "Usage: ./run-tests.sh [options]"
    echo ""
    echo "Options:"
    echo "  --all           Run all test suites"
    echo "  --unit          Run unit tests (JUnit + Mockito)"
    echo "  --integration   Run integration tests (Testcontainers)"
    echo "  --api           Run API tests (Rest Assured)"
    echo "  --contract      Run contract tests (Pact)"
    echo "  --e2e           Run E2E tests (Playwright)"
    echo "  --load          Run load tests (k6)"
    echo "  --health        Run health checks only"
    echo "  --ci            Run all tests in CI mode (with retries)"
    echo "  --parallel      Run independent test suites in parallel"
    echo "  --report        Generate test report"
    echo "  --help          Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  API_BASE_URL    Base URL for API tests (default: http://localhost:8080)"
    echo "  LOAD_VUS        Number of virtual users for load tests (default: 50)"
    echo "  LOAD_DURATION   Duration of load tests in seconds (default: 60)"
}

# Parse arguments
if [ $# -eq 0 ]; then
    show_help
    exit 0
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --all)
            run_all
            shift
            ;;
        --unit)
            run_unit_tests
            shift
            ;;
        --integration)
            run_integration_tests
            shift
            ;;
        --api)
            run_api_tests
            shift
            ;;
        --contract)
            run_contract_tests
            shift
            ;;
        --e2e)
            run_e2e_tests
            shift
            ;;
        --load)
            run_load_tests
            shift
            ;;
        --health)
            run_health_checks
            shift
            ;;
        --ci)
            print_info "Running in CI mode with retries..."
            for i in 1 2 3; do
                print_info "Attempt $i/3"
                EXIT_CODE=0
                run_all
                if [ $EXIT_CODE -eq 0 ]; then
                    break
                fi
                print_info "Retrying in 30 seconds..."
                sleep 30
            done
            shift
            ;;
        --parallel)
            run_parallel
            shift
            ;;
        --report)
            generate_report
            shift
            ;;
        --help)
            show_help
            shift
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

print_header "Test Summary"
echo -e "Total: $TOTAL_TESTS | ${GREEN}Passed: $PASSED_TESTS${NC} | ${RED}Failed: $FAILED_TESTS${NC}"
echo -e "\nReports: $REPORT_DIR"

exit $EXIT_CODE
