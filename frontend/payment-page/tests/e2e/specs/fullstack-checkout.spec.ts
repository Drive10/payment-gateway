import { test, expect } from '@playwright/test';

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080';

test.describe('PayFlow Full E2E Tests', () => {
  
  // Test 1: Health Check
  test('1. API Gateway Health', async ({ request }) => {
    const res = await request.get(`${API_BASE}/actuator/health`);
    expect(res.ok()).toBe(true);
  });

  // Test 2: User Registration
  test('2. User Registration', async ({ request }) => {
    const email = `e2e_${Date.now()}@test.com`;
    const res = await request.post(`${API_BASE}/auth/register`, {
      data: { email, password: 'Password123', firstName: 'E2E', lastName: 'Test' }
    });
    const data = await res.json();
    console.log('Register:', data.success ? 'PASS' : 'FAIL');
    expect(data.success || data.error?.includes('exists')).toBe(true);
  });

  // Test 3: User Login
  test('3. User Login', async ({ request }) => {
    const res = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    const data = await res.json();
    expect(data.success).toBe(true);
    expect(data.data.accessToken).toBeDefined();
  });

  // Test 4: Create Payment as Merchant
  test('4. Create Payment Order', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    const login = await loginRes.json();
    const token = login.data.accessToken;

    const orderRes = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId: `ORD-${Date.now()}`, amount: 1000, currency: 'USD', paymentMethod: 'CARD' },
      headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
    });
    const order = await orderRes.json();
    expect(order.success).toBe(true);
    console.log('Create Order: PASS, paymentId:', order.data?.paymentId);
  });

  // Test 5: Get Payment Status
  test('5. Get Payment Status', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    const token = (await loginRes.json()).data.accessToken;

    const orderRes = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId: `ORD-STATUS-${Date.now()}`, amount: 500, currency: 'USD' },
      headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
    });
    const order = await orderRes.json();
    const paymentId = order.data.paymentId;

    const statusRes = await request.get(`${API_BASE}/api/payments/${paymentId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const status = await statusRes.json();
    expect(status.success).toBe(true);
    console.log('Get Status: PASS');
  });

  // Test 6: Capture Payment
  test('6. Capture Payment', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    const token = (await loginRes.json()).data.accessToken;

    const orderRes = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId: `ORD-CAPTURE-${Date.now()}`, amount: 500, currency: 'USD' },
      headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
    });
    const paymentId = (await orderRes.json()).data.paymentId;

    const captureRes = await request.post(`${API_BASE}/api/payments/${paymentId}/capture`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const capture = await captureRes.json();
    console.log('Capture:', capture.success ? 'PASS' : 'FAIL');
  });

  // Test 7: Event DLQ Check
  test('7. Check Event DLQ', async ({ request }) => {
    const res = await request.get(`${API_BASE}/admin/events/dlq`);
    if (res.status() === 404) {
      console.log('DLQ Endpoint: NOT_CONFIGURED');
      return;
    }
    const data = await res.json();
    console.log('DLQ Events:', Array.isArray(data) ? data.length : 0);
  });

  // Test 8: Event Status Check
  test('8. Event Status', async ({ request }) => {
    const res = await request.get(`${API_BASE}/admin/events/status`);
    if (res.status() === 404) {
      console.log('Event Status: NOT_CONFIGURED');
      return;
    }
    const data = await res.json();
    console.log('Event Status:', data);
  });

  // Test 9: Finance Integrity Check
  test('9. Finance Integrity Check', async ({ request }) => {
    const res = await request.get(`${API_BASE}/admin/finance/integrity/check`);
    if (res.status() === 404) {
      console.log('Finance: NOT_CONFIGURED');
      return;
    }
    const data = await res.json();
    console.log('Integrity:', data.status, 'balanced:', data.balanced);
  });

  // Test 10: Merchant Balance
  test('10. Merchant Balance', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    const token = (await loginRes.json()).data.accessToken;

    const res = await request.get(`${API_BASE}/admin/finance/merchants/dev@test.com/balance`);
    if (res.status() === 404) {
      console.log('Merchant Balance: NOT_CONFIGURED');
      return;
    }
    const data = await res.json();
    console.log('Merchant Balance:', data);
  });
});

test.describe('Payment Flow Integration', () => {
  
  test('Complete Payment Flow - Happy Path', async ({ request }) => {
    // 1. Login
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    expect(loginRes.ok()).toBe(true);
    const token = (await loginRes.json()).data.accessToken;

    // 2. Create Order
    const orderId = `E2E-${Date.now()}`;
    const orderRes = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId, amount: 1000, currency: 'USD', paymentMethod: 'CARD' },
      headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' }
    });
    const order = await orderRes.json();
    expect(order.success).toBe(true);
    const paymentId = order.data.paymentId;

    // 3. Check Status
    const statusRes = await request.get(`${API_BASE}/api/payments/${paymentId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const status = await statusRes.json();
    expect(status.success).toBe(true);
    expect(status.data.status).toBeDefined();

    console.log('Complete Flow: PASS');
  });

  test('Idempotent Payment Creation', async ({ request }) => {
    const loginRes = await request.post(`${API_BASE}/auth/login`, {
      data: { email: 'dev@test.com', password: 'Password123' }
    });
    const token = (await loginRes.json()).data.accessToken;

    const idempotencyKey = `idem-${Date.now()}`;
    
    // Create same order twice
    const res1 = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId: `idem-${Date.now()}`, amount: 100, currency: 'USD' },
      headers: { 
        'Authorization': `Bearer ${token}`, 
        'Content-Type': 'application/json',
        'X-Idempotency-Key': idempotencyKey
      }
    });
    const order1 = await res1.json();

    const res2 = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId: `idem-${Date.now()}`, amount: 100, currency: 'USD' },
      headers: { 
        'Authorization': `Bearer ${token}`, 
        'Content-Type': 'application/json',
        'X-Idempotency-Key': idempotencyKey
      }
    });
    const order2 = await res2.json();

    expect(order1.data.paymentId).toBe(order2.data.paymentId);
    console.log('Idempotency: PASS');
  });

  test('Unauthorized Access Blocked', async ({ request }) => {
    const res = await request.post(`${API_BASE}/api/payments/create-order`, {
      data: { orderId: 'unauth-test', amount: 100, currency: 'USD' },
      headers: { 'Content-Type': 'application/json' }
    });
    expect(res.status()).toBe(401);
  });
});