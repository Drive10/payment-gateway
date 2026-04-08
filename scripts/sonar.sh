#!/bin/bash

# SonarQube Analysis Script for PayFlow (Local)
# Usage: ./scripts/sonar.sh [project-key]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
SONAR_PROJECT_KEY="${1:-PayFlow}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

echo "========================================"
echo "PayFlow SonarQube Analysis"
echo "========================================"
echo "Host URL: $SONAR_HOST_URL"
echo "Project Key: $SONAR_PROJECT_KEY"
echo ""

log_info "Analyzing backend services..."
cd "$PROJECT_ROOT"

mvn sonar:sonar \
    -Dsonar.projectKey="$SONAR_PROJECT_KEY" \
    -Dsonar.projectName="PayFlow" \
    -Dsonar.host.url="$SONAR_HOST_URL" \
    -Dsonar.sources=services,libs \
    -Dsonar.java.source=21 \
    -Dsonar.login=admin \
    -Dsonar.password=admin

echo ""
log_info "Analysis complete!"
echo "View results at: $SONAR_HOST_URL/dashboard?id=$SONAR_PROJECT_KEY"
