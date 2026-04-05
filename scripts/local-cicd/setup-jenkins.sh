#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Jenkins Setup (Docker)                           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo -e "${RED}Docker is not running. Please start Docker.${NC}"
    exit 1
fi

JENKINS_CONTAINER="payment-jenkins"
JENKINS_PORT=8080
AGENT_PORT=50000

# Check if Jenkins is already running
if docker ps --format '{{.Names}}' | grep -q "^${JENKINS_CONTAINER}$"; then
    echo -e "${GREEN}✓ Jenkins is already running${NC}"
    echo -e "${YELLOW}Access Jenkins at: http://localhost:${JENKINS_PORT}${NC}"
    exit 0
fi

# Check if container exists (stopped)
if docker ps -a --format '{{.Names}}' | grep -q "^${JENKINS_CONTAINER}$"; then
    echo -e "${YELLOW}Starting existing Jenkins container...${NC}"
    docker start $JENKINS_CONTAINER
else
    echo -e "${YELLOW}Creating and starting Jenkins container...${NC}"
    
    # Create Docker network if not exists
    docker network create jenkins-network 2>/dev/null || true
    
    # Start Jenkins container
    docker run -d \
        --name $JENKINS_CONTAINER \
        --network jenkins-network \
        -p ${JENKINS_PORT}:8080 \
        -p ${AGENT_PORT}:50000 \
        -v jenkins_home:/var/jenkins_home \
        -v /var/run/docker.sock:/var/run/docker.sock \
        -v "$HOME/.docker:/home/jenkins/.docker" \
        --restart=unless-stopped \
        jenkins/jenkins:lts
    
    # Wait for Jenkins to start
    echo -e "${YELLOW}Waiting for Jenkins to initialize...${NC}"
    sleep 10
    
    # Get initial admin password
    echo -e "${YELLOW}Getting initial admin password...${NC}"
    for i in {1..30}; do
        if docker exec $JENKINS_CONTAINER test -f /var/jenkins_home/secrets/initialAdminPassword; then
            PASSWORD=$(docker exec $JENKINS_CONTAINER cat /var/jenkins_home/secrets/initialAdminPassword)
            echo -e "${GREEN}✓ Jenkins initialized!${NC}"
            break
        fi
        sleep 2
    done
fi

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Jenkins Ready!                                     ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  URL: http://localhost:${JENKINS_PORT}              ║${NC}"
echo -e "${CYAN}║  Username: admin                                    ║${NC}"
echo -e "${CYAN}║  Password: ${PASSWORD:-'check container logs'}${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Next Steps:                                        ║${NC}"
echo -e "${CYAN}║  1. Install required plugins:                        ║${NC}"
echo -e "${CYAN}║     - Docker Pipeline                               ║${NC}"
echo -e "${CYAN}║     - Kubernetes                                     ║${NC}"
echo -e "${CYAN}║     - Git                                           ║${NC}"
echo -e "${CYAN}║  2. Create new Pipeline job                         ║${NC}"
echo -e "${CYAN}║  3. Point to Jenkinsfile in repository              ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

# Show logs if available
if [ -n "$PASSWORD" ]; then
    echo -e "${YELLOW}Initial admin password: $PASSWORD${NC}"
fi
