#!/bin/bash
# ghCR Login Helper Script

echo "=========================================="
echo "GitHub Container Registry Login Helper"
echo "=========================================="
echo ""
echo "Go to: https://github.com/settings/tokens/new"
echo "Create a CLASSIC token with these scopes:"
echo "  - repo (full control)"
echo "  - read:packages"
echo "  - write:packages"
echo "  - delete:packages"
echo ""
echo "Copy the token and paste it below:"
read -s TOKEN

echo ""
echo "Logging in to ghcr.io..."

echo "$TOKEN" | docker login ghcr.io -u Drive10 --password-stdin

if [ $? -eq 0 ]; then
    echo "✅ Login successful!"
    echo ""
    echo "Now testing push..."
    docker tag alpine:latest ghcr.io/drive10/payment-gateway:test
    docker push ghcr.io/drive10/payment-gateway:test
else
    echo "❌ Login failed. Please check your token."
fi
