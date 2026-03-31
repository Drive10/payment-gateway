#!/bin/bash

# Payment Gateway Load Test Script using k6
# Usage: ./load-test.sh [VUS] [DURATION]
# Example: ./load-test.sh 100 60

set -e

VUS=${1:-100}
DURATION=${2:-60}
BASE_URL=${BASE_URL:-http://localhost:8080}

echo "=== Payment Gateway Load Test ==="
echo "Virtual Users: $VUS"
echo "Duration: ${DURATION}s"
echo "Base URL: $BASE_URL"
echo ""

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
    echo "k6 not found. Installing..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install k6
    else
        sudo gpg -k
        sudo apt-get update
        sudo apt-get install -y k6
    fi
fi

# Create test script
cat > /tmp/payment-load-test.js << 'EOF'
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '30s', target: __ENV.VUS || 100 },
    { duration: '1m', target: __ENV.VUS || 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const headers = {
    'Content-Type': 'application/json',
    'X-Idempotency-Key': `test-${Date.now()}-${Math.random()}`,
  };

  // Test payment creation
  const paymentPayload = JSON.stringify({
    amount: Math.floor(Math.random() * 10000) + 100,
    currency: 'USD',
    paymentMethod: {
      type: 'card',
      card: {
        number: '4242424242424242',
        expiryMonth: '12',
        expiryYear: '25',
        cvv: '123',
        cardholderName: 'Test User'
      }
    },
    description: 'Load test payment'
  });

  const paymentRes = http.post(
    `${BASE_URL}/api/v1/payments`,
    paymentPayload,
    { headers }
  );

  check(paymentRes, {
    'payment created': (r) => r.status === 201 || r.status === 200,
    'payment has id': (r) => r.json('data.id') !== undefined,
  }) || errorRate.add(1);

  sleep(1);

  // Test payment retrieval
  if (paymentRes.status() === 201 || paymentRes.status() === 200) {
    const paymentId = paymentRes.json('data.id');
    const getRes = http.get(
      `${BASE_URL}/api/v1/payments/${paymentId}`,
      { headers: { Authorization: paymentRes.headers['Authorization'] } }
    );

    check(getRes, {
      'payment retrieved': (r) => r.status === 200,
    }) || errorRate.add(1);
  }

  sleep(1);
}
EOF

# Run the test
k6 run /tmp/payment-load-test.js \
  --env VUS=$VUS \
  --env DURATION=$DURATION \
  --env BASE_URL=$BASE_URL

echo ""
echo "=== Load Test Complete ==="
