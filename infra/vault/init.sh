#!/bin/sh
set -e

echo "Waiting for Vault to be ready..."
sleep 5

export VAULT_TOKEN="dev-root-token"
export VAULT_ADDR="http://vault:8200"

echo "Enabling KV secrets engine..."
vault secrets enable -path=secret kv 2>/dev/null || echo "KV secrets engine already enabled"

echo "Writing secrets for auth-service..."
vault kv put secret/auth-service \
    db-password="${AUTH_DB_PASSWORD:-payment}" \
    jwt-secret="${JWT_SECRET_B64:-dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNoLXBheW1lbnQta2V5LWJhc2U2NC1lbmNvZGVk}" \
    redis-password="${REDIS_PASSWORD:-redis_dev_pass}" \
    gateway-internal-secret="${GATEWAY_INTERNAL_SECRET:-dev-gateway-internal-secret}"

echo "Writing secrets for payment-service..."
vault kv put secret/payment-service \
    db-password="${PAYMENT_DB_PASSWORD:-payment}" \
    jwt-secret="${JWT_SECRET_B64:-dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNoLXBheW1lbnQta2V5LWJhc2U2NC1lbmNvZGVk}" \
    redis-password="${REDIS_PASSWORD:-redis_dev_pass}" \
    gateway-internal-secret="${GATEWAY_INTERNAL_SECRET:-dev-gateway-internal-secret}"

echo "Writing secrets for order-service..."
vault kv put secret/order-service \
    db-password="${ORDER_DB_PASSWORD:-payment}" \
    jwt-secret="${JWT_SECRET_B64:-dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNoLXBheW1lbnQta2V5LWJhc2U2NC1lbmNvZGVk}" \
    gateway-internal-secret="${GATEWAY_INTERNAL_SECRET:-dev-gateway-internal-secret}"

echo "Writing secrets for notification-service..."
vault kv put secret/notification-service \
    db-password="${NOTIFICATION_DB_PASSWORD:-payment}"

echo "Writing secrets for simulator-service..."
vault kv put secret/simulator-service \
    db-password="${SIMULATOR_DB_PASSWORD:-payment}"

echo "Writing secrets for analytics-service..."
vault kv put secret/analytics-service \
    db-password="${ANALYTICS_DB_PASSWORD:-payment}"

echo "Writing secrets for api-gateway..."
vault kv put secret/api-gateway \
    jwt-secret="${JWT_SECRET_B64:-dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNoLXBheW1lbnQta2V5LWJhc2U2NC1lbmNvZGVk}" \
    redis-password="${REDIS_PASSWORD:-redis_dev_pass}" \
    gateway-internal-secret="${GATEWAY_INTERNAL_SECRET:-dev-gateway-internal-secret}"

echo "Vault initialization complete!"
