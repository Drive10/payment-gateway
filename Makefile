.PHONY: bootstrap doctor smoke up up-full up-hybrid down test build verify compose-check frontend-check payment-local

bootstrap:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 bootstrap

doctor:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 doctor

smoke:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 smoke

up:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 hybrid

up-full:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 full

up-hybrid:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 hybrid

down:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 down

payment-local:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 payment-local

test:
	mvn -B -q test

build:
	mvn -B -q -DskipTests package

verify:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 verify

compose-check:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 compose-check

frontend-check:
	powershell -ExecutionPolicy Bypass -File ./scripts/dev.ps1 frontend-check
