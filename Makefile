.PHONY: up down test build

up:
	docker compose --profile services up -d --build

down:
	docker compose down -v

test:
	mvn -B -q test

build:
	mvn -B -q -DskipTests package
