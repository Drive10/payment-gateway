#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Local Docker Registry Setup                       ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Docker is not running. Please start Docker.${NC}"
    exit 1
fi

# Check if registry is already running
if docker ps -a --format '{{.Names}}' | grep -q "^payment-registry$"; then
    echo -e "${YELLOW}Registry already exists. Starting it...${NC}"
    docker start payment-registry
else
    echo -e "${YELLOW}Starting local Docker registry...${NC}"
    docker run -d \
        --name payment-registry \
        --restart=always \
        -p 5000:5000 \
        -v registry-data:/var/lib/registry \
        registry:2
    
    echo -e "${GREEN}✓ Registry started on port 5000${NC}"
fi

# Configure Docker to allow insecure registry
echo -e "${YELLOW}Configuring Docker for insecure registry...${NC}"

if [ "$(uname)" = "Darwin" ]; then
    echo "Docker Desktop: Go to Settings > Docker Engine and add:"
    echo '{"insecure-registries": ["localhost:5000"]}'
    echo ""
    echo "Alternatively, edit ~/.docker/daemon.json"
fi

# Add insecure registry to daemon config
DOCKER_DAEMON_JSON="$HOME/.docker/daemon.json"
mkdir -p "$HOME/.docker"

if [ -f "$DOCKER_DAEMON_JSON" ]; then
    if grep -q "localhost:5000" "$DOCKER_DAEMON_JSON"; then
        echo -e "${GREEN}✓ Registry already configured${NC}"
    else
        echo -e "${YELLOW}Updating daemon.json...${NC}"
        cat > "$DOCKER_DAEMON_JSON" << 'EOF'
{
  "insecure-registries": ["localhost:5000"],
  "experimental": false,
  "features": {"buildkit": true}
}
EOF
    fi
else
    cat > "$DOCKER_DAEMON_JSON" << 'EOF'
{
  "insecure-registries": ["localhost:5000"],
  "experimental": false,
  "features": {"buildkit": true}
}
EOF
fi

# Restart Docker (if possible)
if command -v docker >/dev/null 2>&1; then
    echo -e "${YELLOW}Note: You may need to restart Docker for changes to take effect.${NC}"
fi

# Pull base images
echo -e "${YELLOW}Pulling base images...${NC}"
docker pull eclipse-temurin:21-jre-alpine || true
docker pull maven:3.9-eclipse-temurin-21-alpine || true
docker pull postgres:16-alpine || true
docker pull redis:7-alpine || true

# Verify registry is working
echo -e "${YELLOW}Verifying registry...${NC}"
sleep 2
if curl -sf http://localhost:5000/v2/ >/dev/null 2>&1; then
    echo -e "${GREEN}✓ Registry is working!${NC}"
else
    echo -e "${RED}✗ Registry verification failed${NC}"
    exit 1
fi

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Local Registry Ready!                               ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Registry URL: localhost:5000                        ║${NC}"
echo -e "${CYAN}║                                                      ║${NC}"
echo -e "${CYAN}║  Test: docker pull hello-world                       ║${NC}"
echo -e "${CYAN}║         docker tag hello-world localhost:5000/test    ║${NC}"
echo -e "${CYAN}║         docker push localhost:5000/test               ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
