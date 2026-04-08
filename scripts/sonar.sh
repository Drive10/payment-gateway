#!/bin/bash

# SonarQube Analysis Script for PayFlow
# Usage: ./scripts/sonar.sh [project-key]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

SONAR_PROJECT_KEY="${1:-drive10-payflow}"

echo "========================================"
echo "PayFlow SonarQube Analysis"
echo "========================================"
echo "Project Key: $SONAR_PROJECT_KEY"
echo ""

# Check if SonarQube is running
if ! docker compose ps sonarqube 2>/dev/null | grep -q "Up"; then
    echo "[INFO] Starting SonarQube..."
    docker compose up -d sonarqube
    echo "[INFO] Waiting for SonarQube to be ready..."
    sleep 60
fi

# Check SonarQube health
echo "[INFO] Checking SonarQube health..."
for i in {1..30}; do
    if curl -sf http://localhost:9000/api/system/health > /dev/null 2>&1; then
        echo "[OK] SonarQube is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

echo ""
echo "========================================"
echo "Analysis Options"
echo "========================================"
echo "1. Backend only (Java)"
echo "2. Frontend only (JS/React)"
echo "3. Full project (Java + React)"
echo "4. Open SonarQube UI"
echo ""

read -p "Select option [1-4]: " option

case "$option" in
    1)
        echo "[INFO] Analyzing backend services..."
        cd "$PROJECT_ROOT"
        mvn sonar:sonar \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
            -Dsonar.projectName="PayFlow Backend" \
            -Dsonar.sources=services,libs \
            -Dsonar.java.source=21 \
            -Dsonar.java.target=21
        ;;
    2)
        echo "[INFO] Analyzing frontend..."
        cd "$PROJECT_ROOT/web/frontend"
        npm run test:coverage
        docker run --rm \
            -v "$PWD:/usr/src" \
            -w /usr/src \
            sonarsource/sonar-scanner-cli \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}-frontend" \
            -Dsonar.projectName="PayFlow Frontend" \
            -Dsonar.sources=src \
            -Dsonar.tests=tests \
            -Dsonar.language=js \
            -Dsonar.js.file.suffixes=.js,.jsx,.ts,.tsx \
            -Dsonar.coverageReportPaths=coverage/lcov.info
        ;;
    3)
        echo "[INFO] Analyzing full project..."
        cd "$PROJECT_ROOT"
        mvn sonar:sonar \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}" \
            -Dsonar.sources=services,libs
        cd "$PROJECT_ROOT/web/frontend"
        npm run test:coverage
        docker run --rm \
            -v "$PWD:/usr/src" \
            -w /usr/src \
            sonarsource/sonar-scanner-cli \
            -Dsonar.projectKey="${SONAR_PROJECT_KEY}-frontend" \
            -Dsonar.sources=src \
            -Dsonar.tests=tests \
            -Dsonar.language=js \
            -Dsonar.js.file.suffixes=.js,.jsx,.ts,.tsx
        ;;
    4)
        echo "[INFO] Opening SonarQube UI..."
        open http://localhost:9000
        ;;
    *)
        echo "[ERROR] Invalid option"
        exit 1
        ;;
esac

echo ""
echo "[OK] Analysis complete!"
echo "View results at: http://localhost:9000/dashboard?id=${SONAR_PROJECT_KEY}"