#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

ENVIRONMENT=${1:-staging}
NAMESPACE="payment-gateway"

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   Deploying to Kubernetes ($ENVIRONMENT)            ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"

# Check kubectl
if ! command -v kubectl >/dev/null 2>&1; then
    echo -e "${RED}kubectl not found. Please install kubectl.${NC}"
    exit 1
fi

# Check cluster connection
echo -e "${YELLOW}Checking cluster connection...${NC}"
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo -e "${RED}Cannot connect to Kubernetes cluster.${NC}"
    echo -e "${YELLOW}For K3s: export KUBECONFIG=/etc/rancher/k3s/k3s.yaml${NC}"
    echo -e "${YELLOW}For kind: kind get clusters${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Connected to cluster${NC}"

# Create namespace if not exists
echo -e "${YELLOW}Creating namespace...${NC}"
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Deploy base resources
echo -e "${YELLOW}Deploying base configurations...${NC}"
kubectl apply -k k8s/base/

# Deploy infrastructure
echo -e "${YELLOW}Deploying infrastructure (Postgres, Redis, Vault)...${NC}"
kubectl apply -k k8s/components/infra/

# Deploy services
echo -e "${YELLOW}Deploying services...${NC}"
kubectl apply -k k8s/components/auth-service/
kubectl apply -k k8s/components/api-gateway/
kubectl apply -k k8s/components/payment-service/
kubectl apply -k k8s/components/order-service/
kubectl apply -k k8s/components/notification-service/
kubectl apply -k k8s/components/analytics-service/
kubectl apply -k k8s/components/simulator-service/

# Wait for deployment
echo -e "${YELLOW}Waiting for deployments...${NC}"
kubectl rollout status deployment/auth-service -n $NAMESPACE --timeout=120s || true
kubectl rollout status deployment/api-gateway -n $NAMESPACE --timeout=120s || true
kubectl rollout status deployment/payment-service -n $NAMESPACE --timeout=120s || true

# Show status
echo -e "${GREEN}✓ Deployment complete!${NC}"
echo ""
echo -e "${CYAN}=== Resources ===${NC}"
kubectl get pods -n $NAMESPACE
echo ""
kubectl get svc -n $NAMESPACE
echo ""

# Get external IP
echo -e "${CYAN}=== External Access ===${NC}"
API_GW_IP=$(kubectl get svc api-gateway -n $NAMESPACE -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "localhost")
echo -e "API Gateway: http://$API_GW_IP"
echo -e "Auth Service: http://$API_GW_IP/api/v1/auth"
echo -e "Payment Service: http://$API_GW_IP/api/v1/payments"

echo -e "${CYAN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Deployment Complete!                                ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════╝${NC}"
