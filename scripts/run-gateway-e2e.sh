#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-http://localhost:8080/api/v1}"
EMAIL="e2e.$(date +%s)@example.com"
PASSWORD="Payflow123!"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-300}"

echo "Using gateway: ${BASE_URL}"
echo "E2E user: ${EMAIL}"

wait_for_health() {
  local name="$1"
  local url="$2"
  local deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    if curl -sS -m 5 "${url}" | jq -e '.status == "UP"' >/dev/null 2>&1; then
      echo "${name} is healthy"
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for ${name} health at ${url}" >&2
  return 1
}

wait_for_health "auth-service" "http://localhost:8081/actuator/health"
wait_for_health "order-service" "http://localhost:8082/actuator/health"
wait_for_health "payment-service" "http://localhost:8083/actuator/health"
wait_for_health "api-gateway" "http://localhost:8080/actuator/health"

register_payload=$(jq -n \
  --arg email "$EMAIL" \
  --arg password "$PASSWORD" \
  '{email:$email,password:$password,firstName:"E2E",lastName:"User"}')

register_response=$(curl -sS -m 30 \
  -X POST "${BASE_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d "${register_payload}")

login_response=$(curl -sS -m 30 \
  -X POST "${BASE_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}")

access_token=$(echo "${login_response}" | jq -r '.data.accessToken // empty')
user_id=$(echo "${login_response}" | jq -r '.data.user.id // empty')

if [[ -z "${access_token}" ]]; then
  echo "Login failed:"
  echo "${login_response}" | jq
  exit 1
fi

external_reference="pay-$(date +%s)-$RANDOM"

order_payload=$(jq -n \
  --arg externalReference "${external_reference}" \
  --arg email "${EMAIL}" \
  --arg userId "${user_id}" \
  '{
    externalReference:$externalReference,
    amount:2499,
    currency:"INR",
    description:"E2E checkout",
    customerEmail:$email,
    customerName:"E2E User",
    userId:($userId|select(length>0))
  }')

order_http_code=$(curl -sS -m 30 \
  -o /tmp/e2e-order-body.json \
  -w "%{http_code}" \
  -X POST "${BASE_URL}/orders" \
  -H "Authorization: Bearer ${access_token}" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: e2e-order-$(date +%s)" \
  -d "${order_payload}")
order_response=$(cat /tmp/e2e-order-body.json)

order_id=$(echo "${order_response}" | jq -r '.data.id // empty')
merchant_id=$(echo "${order_response}" | jq -r '.data.merchantId // empty')

if [[ -z "${order_id}" ]]; then
  echo "Order creation failed: HTTP ${order_http_code}"
  if [[ -n "${order_response}" ]]; then
    echo "${order_response}" | jq .
  else
    echo "(empty response body)"
  fi
  exit 1
fi

if [[ -z "${merchant_id}" ]]; then
  merchant_id="00000000-0000-0000-0000-000000000001"
fi

payment_payload=$(jq -n \
  --arg orderId "${order_id}" \
  --arg merchantId "${merchant_id}" \
  '{
    orderId:$orderId,
    merchantId:$merchantId,
    method:"CARD",
    provider:"RAZORPAY_SIMULATOR",
    transactionMode:"TEST",
    notes:"E2E payment"
  }')

payment_http_code=$(curl -sS -m 30 \
  -o /tmp/e2e-payment-body.json \
  -w "%{http_code}" \
  -X POST "${BASE_URL}/payments" \
  -H "Authorization: Bearer ${access_token}" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: e2e-${external_reference}" \
  -H "X-Request-Id: e2e-payment-$(date +%s)" \
  -d "${payment_payload}")
payment_response=$(cat /tmp/e2e-payment-body.json)

payment_id=$(echo "${payment_response}" | jq -r '.data.id // empty')

if [[ -z "${payment_id}" ]]; then
  echo "Payment creation failed: HTTP ${payment_http_code}"
  if [[ -n "${payment_response}" ]]; then
    echo "${payment_response}" | jq .
  else
    echo "(empty response body)"
  fi
  exit 1
fi

capture_response=$(curl -sS -m 30 \
  -X POST "${BASE_URL}/payments/${payment_id}/capture" \
  -H "Authorization: Bearer ${access_token}" \
  -H "Content-Type: application/json" \
  -H "X-Request-Id: e2e-capture-$(date +%s)" \
  -d '{}')

capture_status=$(echo "${capture_response}" | jq -r '.data.status // empty')

if [[ "${capture_status}" == "PROCESSING" || -z "${capture_status}" ]]; then
  final_payment_response=$(curl -sS -m 30 \
    "${BASE_URL}/payments/${payment_id}" \
    -H "Authorization: Bearer ${access_token}")
else
  final_payment_response="${capture_response}"
fi

order_history_response=$(curl -sS -m 30 \
  "${BASE_URL}/orders?limit=5&offset=0" \
  -H "Authorization: Bearer ${access_token}")

payment_history_response=$(curl -sS -m 30 \
  "${BASE_URL}/payments?limit=5&offset=0" \
  -H "Authorization: Bearer ${access_token}")

echo "REGISTER_OK=$(echo "${register_response}" | jq -r '.success // false')"
echo "LOGIN_OK=$(echo "${login_response}" | jq -r '.success // false')"
echo "ORDER_OK=$(echo "${order_response}" | jq -r '.success // false') ORDER_ID=${order_id}"
echo "PAYMENT_OK=$(echo "${payment_response}" | jq -r '.success // false') PAYMENT_ID=${payment_id}"
echo "CAPTURE_STATUS=$(echo "${final_payment_response}" | jq -r '.data.status // "UNKNOWN"')"
echo "ORDER_HISTORY_OK=$(echo "${order_history_response}" | jq -r '.success // false') COUNT=$(echo "${order_history_response}" | jq -r '.data.content | length')"
echo "PAYMENT_HISTORY_OK=$(echo "${payment_history_response}" | jq -r '.success // false') COUNT=$(echo "${payment_history_response}" | jq -r '.data.content | length')"

echo "--- Final Payment Snapshot ---"
echo "${final_payment_response}" | jq '{
  success,
  data: {
    id: .data.id,
    status: .data.status,
    orderId: .data.orderId,
    method: .data.method,
    provider: .data.provider,
    transactionMode: .data.transactionMode
  }
}'
