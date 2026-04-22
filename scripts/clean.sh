#!/bin/bash
# =============================================================================
# PayFlow Cleanup Script
# Stops and cleans all containers and volumes
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()   { echo -e "${GREEN}[PayFlow]${NC} $1"; }
warn()  { echo -e "${YELLOW}[PayFlow]${NC} $1"; }
error() { echo -e "${RED}[PayFlow]${NC} $1"; }

stop_docker() {
    log "Stopping Docker Compose stacks..."
    docker compose -f docker-compose.full.yml down 2>/dev/null || true
    docker compose -f docker-compose.dev.yml down 2>/dev/null || true
    docker compose -f docker-compose.base.yml down 2>/dev/null || true
}

clean_volumes() {
    warn "Removing volumes..."
    docker volume rm payflow_postgres_data 2>/dev/null || true
}

clean_images() {
    warn "Removing PayFlow images..."
    docker rmi $(docker images -q 'payflow-*' 2>/dev/null) 2>/dev/null || true
}

stop_local() {
    log "Stopping local services..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true
    pkill -f "mvn" 2>/dev/null || true
}

case "${1:-all}" in
    all)
        warn "This will stop and remove all containers, volumes, and images."
        read -p "Continue? [y/N] " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 0
        fi
        
        stop_docker
        clean_volumes
        stop_local
        
        log "Cleanup complete"
        ;;
    docker)
        stop_docker
        clean_volumes
        log "Docker cleanup complete"
        ;;
    local)
        stop_local
        log "Local cleanup complete"
        ;;
    volumes)
        clean_volumes
        log "Volumes cleaned"
        ;;
    images)
        clean_images
        log "Images cleaned"
        ;;
    help|--help|-h)
        echo "PayFlow Cleanup"
        echo ""
        echo "Usage: $0 {all|docker|local|volumes|images}"
        echo ""
        echo "Commands:"
        echo "  all       - Clean everything (docker + local)"
        echo "  docker    - Clean Docker containers and volumes"
        echo "  local     - Stop local services"
        echo "  volumes   - Remove volumes only"
        echo "  images    - Remove PayFlow images"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use: $0 help"
        exit 1
        ;;
esac