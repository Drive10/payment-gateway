.PHONY: up down test build

up:
	docker compose -f deploy/local/docker-compose.yml up -d

down:
	docker compose -f deploy/local/docker-compose.yml down -v

test:
	cd payment-gateway && mvn -B -q test

build:
	cd payment-gateway && mvn -B -q -DskipTests package