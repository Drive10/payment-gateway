#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   K3s Local Cluster Setup                          ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

check_command() {
    if command -v $1 >/dev/null 2>&1; then
        echo -e "${GREEN}✓ $1 is installed${NC}"
        return 0
    else
        echo -e "${RED}✗ $1 is NOT installed${NC}"
        return 1
    fi
}

echo -e "${YELLOW}Checking prerequisites...${NC}"

check_command docker || { echo -e "${RED}Docker is required. Install from https://docker.com${NC}"; exit 1; }
check_command kubectl || { echo -e "${YELLOW}Installing kubectl...${NC}"; }
check_command helm || { echo -e "${YELLOW}Installing helm...${NC}"; }

if ! command -v k3s >/dev/null 2>&1; then
    echo -e "${YELLOW}Installing K3s (lightweight Kubernetes)...${NC}"
    
    if [ "$(uname)" = "Darwin" ]; then
        if command -v brew >/dev/null 2>&1; then
            brew install k3d
            echo -e "${GREEN}✓ Installed k3d (K3s for Docker)${NC}"
            
            echo -e "${YELLOW}Creating K3s cluster...${NC}"
            k3d cluster create payment-gateway \
                --agents 2 \
                --k3s-arg "--disable=traefik@server:0" \
                --port "80:80@loadbalancer" \
                --port "443:443@loadbalancer" \
                --port "6443:6443@loadbalancer" \
                --volume /tmp/k3d:/var/lib/rancher/k3s@all
            
            echo -e "${GREEN}✓ K3s cluster created!${NC}"
            echo -e "${GREEN}✓ Run: kubectl config use-context k3d-payment-gateway${NC}"
            exit 0
        else
            echo -e "${RED}Please install Homebrew first or install Docker Desktop with Kubernetes${NC}"
            exit 1
        fi
    else
        curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC="--write-kubeconfig-mode 644 --disable=traefik" sh
        
        echo -e "${GREEN}✓ K3s installed!${NC}"
        echo -e "${GREEN}✓ Kubeconfig at: /etc/rancher/k3s/k3s.yaml${NC}"
        
        export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
        echo "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml" >> ~/.bashrc
    fi
else
    echo -e "${GREEN}✓ K3s is already installed${NC}"
fi

if command -v kubectl >/dev/null 2>&1; then
    echo -e "${YELLOW}Checking cluster status...${NC}"
    kubectl get nodes
    kubectl get pods -A
fi

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Next Steps:                                       ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  1. kubectl get nodes                              ║${NC}"
echo -e "${CYAN}║  2. scripts/local-cicd/setup-registry.sh           ║${NC}"
echo -e "${CYAN}║  3. scripts/local-cicd/setup-jenkins.sh            ║${NC}"
echo -e "${CYAN}║  4. k8s/deploy.sh staging                          ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
