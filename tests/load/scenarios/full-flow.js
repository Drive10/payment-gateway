import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const authDuration = new Trend('auth_duration');
const orderDuration = new Trend('order_duration');
const paymentDuration = new Trend('payment_duration');

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 50 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.1'],
    auth_duration: ['p(95)<300'],
    order_duration: ['p(95)<400'],
    payment_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function registerAndLogin() {
  const timestamp = Date.now();
  const email = `load-${timestamp}-${Math.random().toString(36).substring(7)}@example.com`;
  const password = 'LoadTest123!';

  const registerPayload = JSON.stringify({
    email: email,
    password: password,
    firstName: 'Load',
    lastName: 'Tester'
  });

  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    registerPayload,
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(registerRes, {
    'register: status 200': (r) => r.status === 200,
    'register: has accessToken': (r) => {
      try { return r.json('accessToken') !== undefined; }
      catch { return false; }
    },
  }) || errorRate.add(1);

  if (registerRes.status !== 200) return null;

  return registerRes.json('accessToken');
}

export default function () {
  const token = registerAndLogin();
  if (!token) {
    errorRate.add(1);
    sleep(2);
    return;
  }

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  };

  group('Order Flow', function () {
    const orderPayload = JSON.stringify({
      amount: Math.floor(Math.random() * 50000) + 1000,
      currency: 'USD',
      customerEmail: 'customer@loadtest.com',
      description: 'Load test order'
    });

    const orderStart = Date.now();
    const orderRes = http.post(
      `${BASE_URL}/api/v1/orders`,
      orderPayload,
      { headers: authHeaders }
    );
    authDuration.add(Date.now() - orderStart);

    check(orderRes, {
      'order: status 200': (r) => r.status === 200,
      'order: has id': (r) => {
        try { return r.json('id') !== undefined; }
        catch { return false; }
      },
    }) || errorRate.add(1);

    if (orderRes.status !== 200) return;

    const orderId = orderRes.json('id');

    group('Payment Flow', function () {
      const paymentPayload = JSON.stringify({
        orderId: orderId,
        amount: orderRes.json('amount'),
        currency: 'USD',
        provider: 'STRIPE',
        paymentMethod: 'CARD'
      });

      const idempotencyKey = `load-${Date.now()}-${Math.random().toString(36).substring(7)}`;

      const paymentStart = Date.now();
      const paymentRes = http.post(
        `${BASE_URL}/api/v1/payments`,
        paymentPayload,
        {
          headers: {
            ...authHeaders,
            'Idempotency-Key': idempotencyKey
          }
        }
      );
      paymentDuration.add(Date.now() - paymentStart);

      check(paymentRes, {
        'payment: status 200': (r) => r.status === 200,
        'payment: has id': (r) => {
          try { return r.json('id') !== undefined; }
          catch { return false; }
        },
      }) || errorRate.add(1);

      if (paymentRes.status === 200) {
        const paymentId = paymentRes.json('id');

        const captureRes = http.post(
          `${BASE_URL}/api/v1/payments/${paymentId}/capture`,
          JSON.stringify({ amount: paymentRes.json('amount') }),
          { headers: authHeaders }
        );

        check(captureRes, {
          'capture: status 200': (r) => r.status === 200,
        }) || errorRate.add(1);
      }
    });

    group('List Operations', function () {
      http.get(`${BASE_URL}/api/v1/orders`, { headers: authHeaders });
      http.get(`${BASE_URL}/api/v1/payments`, { headers: authHeaders });
    });
  });

  sleep(2);
}
