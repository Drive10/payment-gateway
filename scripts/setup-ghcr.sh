#!/bin/bash
set -e

echo "=========================================="
echo "GitHub Container Registry Setup"
echo "=========================================="

echo ""
echo "=== Step 1: Generate GitHub Token ==="
echo "Go to: https://github.com/settings/tokens/new"
echo "Required scopes:"
echo "  - read:packages"
echo "  - write:packages"
echo ""
echo "Create a classic token and paste it below:"
read -s GITHUB_TOKEN

echo ""
echo "=== Step 2: Login to ghCR ==="
echo "$GITHUB_TOKEN" | docker login ghcr.io -u "$GITHUB_USER" --password-stdin

echo ""
echo "=== Step 3: Test Push ==="
echo "Testing with a simple image..."

# Create test image
docker pull alpine:latest
docker tag alpine:latest ghcr.io/drive10/payment-gateway:test

# Push test image
docker push ghcr.io/drive10/payment-gateway:test

echo ""
echo "=========================================="
echo "ghCR Setup Complete!"
echo "=========================================="
echo ""
echo "Your registry: ghcr.io/drive10/payment-gateway"
echo ""
echo "To use in docker-compose:"
echo "  REGISTRY=ghcr.io docker compose -f docker-compose.prod.yml up -d"
echo ""
echo "To use in Jenkins, add credential 'ghcr-token' with your GitHub token"
