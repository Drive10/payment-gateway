#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Jenkins Setup for Mac (Local Production)         ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

JENKINS_HOME="$HOME/jenkins_home"
JENKINS_PORT=8080

# Check prerequisites
echo -e "${YELLOW}Checking prerequisites...${NC}"

if ! command -v java >/dev/null 2>&1; then
    echo -e "${RED}Java not found. Install Java 17+ first.${NC}"
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    echo -e "${RED}Docker not found. Install Docker Desktop first.${NC}"
    exit 1
fi

if ! command -v git >/dev/null 2>&1; then
    echo -e "${RED}Git not found.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites met${NC}"

# Create Jenkins home
mkdir -p "$JENKINS_HOME"

# Check if Jenkins is already running
if pgrep -f "jenkins.war" >/dev/null 2>&1; then
    echo -e "${YELLOW}Jenkins is already running.${NC}"
    echo -e "${YELLOW}Access at: http://localhost:${JENKINS_PORT}${NC}"
    exit 0
fi

# Download Jenkins war if not exists
JENKINS_WAR="$JENKINS_HOME/jenkins.war"
if [ ! -f "$JENKINS_WAR" ]; then
    echo -e "${YELLOW}Downloading Jenkins...${NC}"
    curl -L -o "$JENKINS_WAR" https://get.jenkins.io/war-stable/latest/jenkins.war
    echo -e "${GREEN}✓ Jenkins downloaded${NC}"
fi

# Start Jenkins
echo -e "${YELLOW}Starting Jenkins...${NC}"
nohup java \
    -DJENKINS_HOME="$JENKINS_HOME" \
    -jar "$JENKINS_WAR" \
    --httpPort=$JENKINS_PORT \
    --httpListenAddress=127.0.0.1 \
    > "$JENKINS_HOME/jenkins.log" 2>&1 &

echo $! > "$JENKINS_HOME/jenkins.pid"
echo -e "${GREEN}✓ Jenkins started (PID: $!)${NC}"

# Wait for Jenkins to initialize
echo -e "${YELLOW}Waiting for Jenkins to initialize...${NC}"
for i in {1..60}; do
    if curl -sf http://localhost:$JENKINS_PORT/login >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Jenkins is ready!${NC}"
        break
    fi
    sleep 2
done

# Get initial admin password
if [ -f "$JENKINS_HOME/secrets/initialAdminPassword" ]; then
    INITIAL_PASSWORD=$(cat "$JENKINS_HOME/secrets/initialAdminPassword")
    echo -e "${GREEN}Initial Admin Password: $INITIAL_PASSWORD${NC}"
fi

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Jenkins Ready!                                     ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  URL: http://localhost:${JENKINS_PORT}              ║${NC}"
echo -e "${CYAN}║  Jenkins Home: $JENKINS_HOME                        ║${NC}"
echo -e "${CYAN}║  Logs: tail -f $JENKINS_HOME/jenkins.log            ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Next Steps:                                        ║${NC}"
echo -e "${CYAN}║  1. Open http://localhost:${JENKINS_PORT}           ║${NC}"
echo -e "${CYAN}║  2. Enter initial admin password above              ║${NC}"
echo -e "${CYAN}║  3. Install suggested plugins                       ║${NC}"
echo -e "${CYAN}║  4. Create admin user                               ║${NC}"
echo -e "${CYAN}║  5. Install required plugins:                       ║${NC}"
echo -e "${CYAN}║     - Docker Pipeline                               ║${NC}"
echo -e "${CYAN}║     - Git                                           ║${NC}"
echo -e "${CYAN}║     - GitHub Branch Source                          ║${NC}"
echo -e "${CYAN}║     - Pipeline                                      ║${NC}"
echo -e "${CYAN}║  6. Add credentials (see setup-credentials.sh)     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
