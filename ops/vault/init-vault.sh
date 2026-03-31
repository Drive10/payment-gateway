#!/bin/bash

set -e

export VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-}"

echo "=== Vault Initialization Script ==="
echo "VAULT_ADDR: $VAULT_ADDR"

if [ -n "$VAULT_TOKEN" ]; then
  echo "Using provided Vault token"
  exit 0
fi

if vault status 2>/dev/null | grep -q "Initialized.*false"; then
  echo "Initializing Vault..."
  
  INIT_OUTPUT=$(vault operator init -key-shares=5 -key-threshold=3 -format=json)
  
  UNSEAL_KEYS=$(echo "$INIT_OUTPUT" | jq -r '.unseal_keys_b64[]')
  ROOT_TOKEN=$(echo "$INIT_OUTPUT" | jq -r '.root_token')
  
  echo "$INIT_OUTPUT" > /tmp/vault-init.json
  
  echo "Unsealing Vault..."
  for key in $UNSEAL_KEYS; do
    vault operator unseal "$key"
  done
  
  export VAULT_TOKEN="$ROOT_TOKEN"
  
  echo "Vault initialized and unsealed"
  echo "Root token saved to /tmp/vault-init.json"
  
  echo ""
  echo "=== Configuring Secrets ==="
  
  vault secrets enable -path=secret -version=2 kv 2>/dev/null || true
  vault secrets enable -path=database -version=2 kv 2>/dev/null || true
  vault secrets enable -path=aws aws 2>/dev/null || true
  
  echo ""
  echo "=== Creating Service Policies ==="
  
  for service in auth order payment notification webhook settlement risk analytics merchant dispute simulator; do
    cat > /tmp/${service}-policy.hcl << EOF
path "secret/data/${service}/*" {
  capabilities = ["read", "list"]
}

path "database/creds/${service}-role" {
  capabilities = ["read"]
}

path "aws/creds/${service}-role" {
  capabilities = ["read"]
}
EOF
    vault policy write ${service}-service /tmp/${service}-policy.hcl
  done
  
  echo ""
  echo "=== Enabling Database Secrets Engine ==="
  vault secrets enable -path=database database
  
  echo ""
  echo "=== Creating Example Secrets ==="
  
  vault kv put secret/database/auth \
    username=auth \
    password=authpass
  
  vault kv put secret/database/order \
    username=order \
    password=orderpass
  
  vault kv put secret/database/payment \
    username=payment \
    password=paymentpass
  
  vault kv put secret/jwt/secret \
    value="dGhpcy1pcy1hLXZlcnktc2VjdXJlLWp3dC1zZWNyZXQta2V5LWZvci1maW50ZWNoLXBheW1lbnQta2V5LWJhc2U2NC1lbmNvZGVk"
  
  vault kv put secret/api-keys/stripe \
    api_key="sk_test_xxxxxxxxxxxxx" \
    webhook_secret="whsec_xxxxxxxxxxxxx"
  
  vault kv put secret/api-keys/paypal \
    client_id="xxxxxxxxxxxxx" \
    client_secret="xxxxxxxxxxxxx"
  
  vault kv put secret/api-keys/square \
    access_token="sq0atp-xxxxxxxxxxxxx" \
    application_id="sandbox-xxxxxxxxxxxxx"
  
  echo ""
  echo "=== Vault is Ready ==="
  echo "Token: $ROOT_TOKEN"
  echo ""
  echo "To use in other sessions:"
  echo "  export VAULT_TOKEN=$ROOT_TOKEN"
  
else
  if vault status 2>/dev/null | grep -q "Sealed.*true"; then
    echo "Vault is sealed. Provide unseal keys:"
    read -s -p "Unseal Key: " UNSEAL_KEY
    vault operator unseal "$UNSEAL_KEY"
  else
    echo "Vault already initialized"
  fi
fi