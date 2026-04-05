#!/bin/bash
set -e

echo "=== Payment Gateway Build Script ==="
echo "Building all services in parallel using Docker Bake..."

# Build all services in parallel
docker buildx bake --push --progress=plain 2>&1 | tail -30

echo ""
echo "=== Build Complete ==="
echo "All service images built and pushed to registry"
