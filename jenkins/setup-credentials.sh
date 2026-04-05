#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Jenkins Credentials Setup                        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

JENKINS_HOME="$HOME/jenkins_home"
JENKINS_URL="http://localhost:8080"

# Check if Jenkins is running
if ! curl -sf "$JENKINS_URL/login" >/dev/null 2>&1; then
    echo -e "${RED}Jenkins is not running. Start it first with setup-jenkins.sh${NC}"
    exit 1
fi

# Get Jenkins CLI jar
JENKINS_CLI="$JENKINS_HOME/jenkins-cli.jar"
if [ ! -f "$JENKINS_CLI" ]; then
    echo -e "${YELLOW}Downloading Jenkins CLI...${NC}"
    curl -L -o "$JENKINS_CLI" "$JENKINS_URL/jnlpJars/jenkins-cli.jar" 2>/dev/null || echo "CLI download skipped (manual setup required)"
fi

echo ""
echo -e "${YELLOW}This script will guide you through setting up credentials in Jenkins.${NC}"
echo -e "${YELLOW}You'll need to add these credentials manually through the Jenkins UI:${NC}"
echo ""

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Required Credentials                               ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo ""

echo -e "${GREEN}1. GitHub Credentials:${NC}"
echo -e "   Go to: Jenkins > Manage Jenkins > Credentials > System > Global credentials"
echo -e "   Add Kind: Username with password"
echo -e "   ID: github-credentials"
echo -e "   Username: YOUR_GITHUB_USERNAME"
echo -e "   Password: YOUR_GITHUB_TOKEN (PAT with repo access)"
echo ""

echo -e "${GREEN}2. Docker Registry Credentials:${NC}"
echo -e "   Add Kind: Username with password"
echo -e "   ID: docker-registry"
echo -e "   Username: admin"
echo -e "   Password: admin"
echo ""

echo -e "${GREEN}3. Database Passwords (Secret text):${NC}"
echo -e "   Add Kind: Secret text"
echo -e "   ID: db-password"
echo -e "   Secret: payment_dev_pass"
echo ""

echo -e "${GREEN}4. Redis Password (Secret text):${NC}"
echo -e "   Add Kind: Secret text"
echo -e "   ID: redis-password"
echo -e "   Secret: redis_dev_pass"
echo ""

echo -e "${GREEN}5. JWT Secret (Secret text):${NC}"
echo -e "   Add Kind: Secret text"
echo -e "   ID: jwt-secret"
echo -e "   Secret: $(openssl rand -base64 32)"
echo ""

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Jenkinsfile will use these credential IDs:         ║${NC}"
echo -e "${CYAN}║  - github-credentials                               ║${NC}"
echo -e "${CYAN}║  - docker-registry                                  ║${NC}"
echo -e "${CYAN}║  - db-password                                      ║${NC}"
echo -e "${CYAN}║  - redis-password                                   ║${NC}"
echo -e "${CYAN}║  - jwt-secret                                       ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
