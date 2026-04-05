#!/bin/bash
set -e

CYAN='\033[36m'
GREEN='\033[32m'
YELLOW='\033[33m'
RED='\033[31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                                                              ║${NC}"
echo -e "${CYAN}║          Payment Gateway - Local Production Setup           ║${NC}"
echo -e "${CYAN}║                                                              ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"

show_help() {
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  all           - Setup everything (K3s + Registry + Jenkins + Deploy)"
    echo "  k3s           - Setup Kubernetes (K3s)"
    echo "  registry      - Setup local Docker registry"
    echo "  jenkins       - Setup Jenkins"
    echo "  deploy        - Deploy to Kubernetes"
    echo "  build         - Build and push Docker images"
    echo "  monitoring    - Deploy Prometheus + Grafana"
    echo "  ingress       - Deploy NGINX Ingress Controller"
    echo "  status        - Show status of all components"
    echo "  clean         - Clean up all local resources"
    echo ""
}

check_docker() {
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}Docker is not running. Please start Docker Desktop.${NC}"
        exit 1
    fi
}

setup_k3s() {
    echo -e "${YELLOW}Setting up K3s...${NC}"
    check_docker
    
    if command -v k3d >/dev/null 2>&1; then
        echo "Creating K3s cluster..."
        k3d cluster create payment-gateway \
            --agents 2 \
            --port "80:80@loadbalancer" \
            --port "443:443@loadbalancer" \
            --port "5000:5000@loadbalancer" \
            --port "6443:6443@loadbalancer"
        
        kubectl config use-context k3d-payment-gateway
        echo -e "${GREEN}✓ K3s cluster created${NC}"
    else
        echo -e "${RED}k3d not found. Please install: brew install k3d${NC}"
        exit 1
    fi
}

setup_registry() {
    echo -e "${YELLOW}Setting up Docker registry...${NC}"
    check_docker
    
    if docker ps -a --format '{{.Names}}' | grep -q "^payment-registry$"; then
        docker start payment-registry
    else
        docker run -d \
            --name payment-registry \
            --restart=always \
            -p 5000:5000 \
            -v registry-data:/var/lib/registry \
            registry:2
    fi
    
    echo -e "${GREEN}✓ Registry running on localhost:5000${NC}"
}

setup_jenkins() {
    echo -e "${YELLOW}Setting up Jenkins...${NC}"
    check_docker
    
    if docker ps --format '{{.Names}}' | grep -q "^payment-jenkins$"; then
        echo "Jenkins already running"
    else
        docker run -d \
            --name payment-jenkins \
            -p 8080:8080 \
            -p 50000:50000 \
            -v jenkins_home:/var/jenkins_home \
            -v /var/run/docker.sock:/var/run/docker.sock \
            --restart=unless-stopped \
            jenkins/jenkins:lts
        
        echo -e "${YELLOW}Waiting for Jenkins to initialize...${NC}"
        sleep 15
    fi
    
    echo -e "${GREEN}✓ Jenkins ready at http://localhost:8080${NC}"
}

build_images() {
    echo -e "${YELLOW}Building Docker images...${NC}"
    
    SERVICES=(
        "api-gateway"
        "auth-service"
        "order-service"
        "payment-service"
        "notification-service"
        "analytics-service"
        "simulator-service"
    )
    
    for svc in "${SERVICES[@]}"; do
        echo "Building $svc..."
        docker build \
            --build-arg SERVICE_PATH=services/$svc \
            -t localhost:5000/$svc:latest \
            -f Dockerfile.build .
        
        docker push localhost:5000/$svc:latest
    done
    
    echo -e "${GREEN}✓ All images built and pushed${NC}"
}

deploy_all() {
    echo -e "${YELLOW}Deploying to Kubernetes...${NC}"
    
    # Create namespace
    kubectl create namespace payment-gateway --dry-run=client -o yaml | kubectl apply -f -
    kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
    
    # Deploy base
    kubectl apply -k k8s/base/
    
    # Deploy ingress first
    echo -e "${YELLOW}Deploying Ingress Controller...${NC}"
    kubectl apply -f k8s/components/ingress/
    
    # Deploy infra
    kubectl apply -k k8s/components/infra/
    
    # Deploy services
    for dir in k8s/components/*/; do
        if [ -f "$dir/deployment.yaml" ]; then
            kubectl apply -f "$dir"
        fi
    done
    
    # Deploy monitoring
    echo -e "${YELLOW}Deploying monitoring...${NC}"
    kubectl apply -f k8s/components/monitoring/prometheus.yaml
    kubectl apply -f k8s/components/monitoring/prometheus-deployment.yaml
    kubectl apply -f k8s/components/monitoring/grafana-config.yaml
    kubectl apply -f k8s/components/monitoring/grafana-deployment.yaml
    
    # Wait for pods
    echo "Waiting for pods..."
    sleep 30
    kubectl get pods -n payment-gateway
    kubectl get pods -n monitoring
    
    echo -e "${GREEN}✓ Deployment complete!${NC}"
}

show_status() {
    echo -e "${CYAN}=== Docker Containers ===${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    echo ""
    echo -e "${CYAN}=== Kubernetes Pods (payment-gateway) ===${NC}"
    kubectl get pods -n payment-gateway 2>/dev/null || echo "No pods running"
    
    echo ""
    echo -e "${CYAN}=== Kubernetes Pods (monitoring) ===${NC}"
    kubectl get pods -n monitoring 2>/dev/null || echo "No pods running"
    
    echo ""
    echo -e "${CYAN}=== Kubernetes Services ===${NC}"
    kubectl get svc -n payment-gateway 2>/dev/null || echo "No services running"
}

deploy_monitoring() {
    echo -e "${YELLOW}Deploying Prometheus + Grafana...${NC}"
    
    kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
    kubectl apply -f k8s/components/monitoring/prometheus.yaml
    kubectl apply -f k8s/components/monitoring/prometheus-deployment.yaml
    kubectl apply -f k8s/components/monitoring/grafana-config.yaml
    kubectl apply -f k8s/components/monitoring/grafana-deployment.yaml
    
    echo -e "${GREEN}✓ Monitoring deployed!${NC}"
}

deploy_ingress() {
    echo -e "${YELLOW}Deploying NGINX Ingress Controller...${NC}"
    
    kubectl apply -f k8s/components/ingress/ingress-controller.yaml
    
    echo -e "${GREEN}✓ Ingress Controller deployed!${NC}"
}

clean_all() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    
    # Delete K8s resources
    kubectl delete namespace payment-gateway 2>/dev/null || true
    
    # Stop containers
    docker stop payment-jenkins payment-registry 2>/dev/null || true
    docker rm payment-jenkins payment-registry 2>/dev/null || true
    
    # Delete K3s cluster
    k3d cluster delete payment-gateway 2>/dev/null || true
    
    echo -e "${GREEN}✓ Cleanup complete${NC}"
}

# Main
case "${1:-all}" in
    all)
        setup_k3s
        setup_registry
        setup_jenkins
        build_images
        deploy_all
        show_status
        ;;
    k3s)
        setup_k3s
        ;;
    registry)
        setup_registry
        ;;
    jenkins)
        setup_jenkins
        ;;
    deploy)
        deploy_all
        ;;
    build)
        build_images
        ;;
    monitoring)
        deploy_monitoring
        ;;
    ingress)
        deploy_ingress
        ;;
    status)
        show_status
        ;;
    clean)
        clean_all
        ;;
    *)
        show_help
        ;;
esac

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║  Setup Complete!                                           ║${NC}"
echo -e "${CYAN}╠══════════════════════════════════════════════════════════════╣${NC}"
echo -e "${CYAN}║  Access Points:                                            ║${NC}"
echo -e "${CYAN}║  - Jenkins:    http://localhost:8080                       ║${NC}"
echo -e "${CYAN}║  - Registry:   localhost:5000                               ║${NC}"
echo -e "${CYAN}║  - API Gateway: http://localhost (after deploy)           ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
