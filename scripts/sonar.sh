#!/bin/bash

# SonarCloud Analysis Script for PayFlow
# Usage: ./scripts/sonar.sh [project-key]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

SONAR_PROJECT_KEY="${1:-Drive10_payflow}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

echo "========================================"
echo "PayFlow SonarCloud Analysis"
echo "========================================"
echo "Project Key: $SONAR_PROJECT_KEY"
echo ""

# Check for SONAR_TOKEN
if [ -z "$SONAR_TOKEN" ]; then
    log_warn "SONAR_TOKEN not set in environment"
    echo "Get your token from:"
    echo "  https://sonarcloud.io/account/security"
    echo ""
    echo "Set it with:"
    echo "  export SONAR_TOKEN=your-token"
    echo ""
    read -p "Enter SONAR_TOKEN now (or press Enter to skip): " token
    if [ -n "$token" ]; then
        export SONAR_TOKEN="$token"
    else
        log_warn "Skipping SonarCloud analysis"
        echo "To enable, set SONAR_TOKEN environment variable"
    fi
fi

if [ -z "$SONAR_TOKEN" ]; then
    log_warn "No SONAR_TOKEN found. Opening SonarCloud..."
    open https://sonarcloud.io
    exit 0
fi

echo "========================================"
echo "Analysis Options"
echo "========================================"
echo "1. Backend only (Java)"
echo "2. Frontend only (JS/React)"
echo "3. Full project (Java + React)"
echo "4. Open SonarCloud Dashboard"
echo ""

read -p "Select option [1-4]: " option

case "$option" in
    1)
        log_info "Analyzing backend services..."
        cd "$PROJECT_ROOT"
        mvn sonar:sonar \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
            -Dsonar.projectName="PayFlow Backend" \
            -Dsonar.organization=drive10 \
            -Dsonar.token="$SONAR_TOKEN" \
            -Dsonar.sources=services,libs \
            -Dsonar.java.source=21
        ;;
    2)
        log_info "Analyzing frontend..."
        cd "$PROJECT_ROOT/web/frontend"
        npm run test:coverage 2>/dev/null || true
        
        docker run --rm \
            -e SONAR_TOKEN="$SONAR_TOKEN" \
            -v "$PWD:/usr/src" \
            -w /usr/src \
            sonarsource/sonar-scanner-cli \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}-frontend" \
            -Dsonar.projectName="PayFlow Frontend" \
            -Dsonar.organization=drive10 \
            -Dsonar.sources=src \
            -Dsonar.tests=tests \
            -Dsonar.language=js \
            -Dsonar.js.file.suffixes=.js,.jsx,.ts,.tsx
        ;;
    3)
        log_info "Analyzing full project..."
        cd "$PROJECT_ROOT"
        mvn sonar:sonar \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
            -Dsonar.organization=drive10 \
            -Dsonar.token="$SONAR_TOKEN" \
            -Dsonar.sources=services,libs \
            -Dsonar.java.source=21
        
        cd "$PROJECT_ROOT/web/frontend"
        npm run test:coverage 2>/dev/null || true
        
        docker run --rm \
            -e SONAR_TOKEN="$SONAR_TOKEN" \
            -v "$PWD:/usr/src" \
            -w /usr/src \
            sonarsource/sonar-scanner-cli \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}-frontend" \
            -Dsonar.organization=drive10 \
            -Dsonar.sources=src \
            -Dsonar.tests=tests \
            -Dsonar.language=js
        ;;
    4)
        log_info "Opening SonarCloud..."
        open "https://sonarcloud.io/dashboard?id=${SONAR_PROJECT_KEY}"
        ;;
    *)
        echo "[ERROR] Invalid option"
        exit 1
        ;;
esac

echo ""
echo "[OK] Analysis complete!"
echo "View results at: https://sonarcloud.io/dashboard?id=${SONAR_PROJECT_KEY}"