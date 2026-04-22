.PHONY: help local docker local-stop docker-stop clean build test lint format

help:
	@echo "PayFlow - Development Commands"
	@echo ""
	@echo "  LOCAL MODE (Infra in Docker, services local)"
	@echo "    make local        - Start infrastructure + services locally"
	@echo "    make local-stop   - Stop local services"
	@echo ""
	@echo "  DOCKER MODE (Full stack in Docker)"
	@echo "    make docker       - Start full stack in Docker"
	@echo "    make docker-stop - Stop Docker stack"
	@echo ""
	@echo "  CLEANUP"
	@echo "    make clean      - Stop all and clean volumes"
	@echo ""
	@echo "  BUILD & TEST"
	@echo "    make build     - Build all services"
	@echo "    make test     - Run tests"
	@echo "    make lint    - Run linters"

local:
	./scripts/dev.sh start

local-stop:
	./scripts/dev.sh stop

docker:
	./scripts/docker.sh start

docker-stop:
	./scripts/docker.sh stop

clean:
	./scripts/clean.sh all

build:
	mvn clean package -DskipTests

test:
	mvn test

lint:
	mvn spotless:check
	cd frontend/payment-page && npm run lint || true

format:
	mvn spotless:apply
	cd frontend/payment-page && npm run format || true