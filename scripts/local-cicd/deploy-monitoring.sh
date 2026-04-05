#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Deploy Monitoring Stack (Prometheus + Grafana)   ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

# Check kubectl
if ! command -v kubectl >/dev/null 2>&1; then
    echo -e "${RED}kubectl not found${NC}"
    exit 1
fi

# Deploy monitoring namespace and configs
echo -e "${YELLOW}Deploying Prometheus...${NC}"
kubectl apply -f k8s/components/monitoring/prometheus.yaml
kubectl apply -f k8s/components/monitoring/prometheus-deployment.yaml

echo -e "${YELLOW}Deploying Grafana...${NC}"
kubectl apply -f k8s/components/monitoring/grafana-config.yaml
kubectl apply -f k8s/components/monitoring/grafana-deployment.yaml

echo -e "${YELLOW}Waiting for monitoring pods...${NC}"
sleep 10

echo -e "${GREEN}✓ Monitoring stack deployed!${NC}"
echo ""
echo -e "${CYAN}=== Monitoring Services ===${NC}"
kubectl get svc -n monitoring

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Access:                                          ║${NC}"
echo -e "${CYAN}║  - Prometheus: kubectl port-forward -n monitoring svc/prometheus 9090:9090${NC}"
echo -e "${CYAN}║  - Grafana:    kubectl port-forward -n monitoring svc/grafana 3000:3000${NC}"
echo -e "${CYAN}║  - Default login: admin / admin123                 ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
