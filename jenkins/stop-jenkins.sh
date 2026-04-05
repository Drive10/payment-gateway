#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Stop Jenkins                                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

JENKINS_HOME="$HOME/jenkins_home"

if [ -f "$JENKINS_HOME/jenkins.pid" ]; then
    PID=$(cat "$JENKINS_HOME/jenkins.pid")
    if kill -0 "$PID" 2>/dev/null; then
        echo -e "${YELLOW}Stopping Jenkins (PID: $PID)...${NC}"
        kill "$PID"
        echo -e "${GREEN}✓ Jenkins stopped${NC}"
    else
        echo -e "${YELLOW}Jenkins process not running (stale PID file)${NC}"
        rm -f "$JENKINS_HOME/jenkins.pid"
    fi
else
    # Try to find Jenkins process
    JENKINS_PID=$(pgrep -f "jenkins.war" 2>/dev/null || true)
    if [ -n "$JENKINS_PID" ]; then
        echo -e "${YELLOW}Stopping Jenkins (PID: $JENKINS_PID)...${NC}"
        kill "$JENKINS_PID"
        echo -e "${GREEN}✓ Jenkins stopped${NC}"
    else
        echo -e "${YELLOW}Jenkins is not running${NC}"
    fi
fi
