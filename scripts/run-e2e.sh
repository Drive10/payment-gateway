#!/usr/bin/env bash
set -euo pipefail

echo "Starting End-to-End harness..."

# Start infrastructure (infra profile)
docker compose --profile infra up -d
echo "Waiting for infra to be ready..."
sleep 20

# Run tests with RUN_E2E=true to enable E2E harness in test class
RUN_E2E=true RUNNING=1 mvn -Dtest=dev.payment.paymentservice.e2e.EndToEndPaymentFlowTest test

# Teardown
docker compose --profile infra down -v

echo "End-to-End harness finished."
