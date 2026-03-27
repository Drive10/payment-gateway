.PHONY: up up-full up-hybrid down test build verify compose-check

up:
	docker compose -f docker-compose.yml -f docker-compose.override.yml --profile services up -d --build

up-full:
	docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services --profile full --profile optional up -d --build

up-hybrid:
	docker compose -f docker-compose.yml -f docker-compose.override.yml --profile services --profile optional up -d --build

down:
	docker compose down -v

test:
	mvn -B -q test

build:
	mvn -B -q -DskipTests package

verify:
	mvn -B -q verify

compose-check:
	docker compose -f docker-compose.yml -f docker-compose.override.yml --profile services config > NUL
	docker compose -f docker-compose.yml -f docker-compose.docker.yml --profile services --profile full --profile optional config > NUL
