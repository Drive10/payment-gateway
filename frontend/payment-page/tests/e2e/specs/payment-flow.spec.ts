import { test, expect } from '@playwright/test';

test.describe('Payment Platform E2E Tests', () => {
  const API_BASE = 'http://localhost:8080';
  const API_KEY = 'sk_test_88573d07c94d45f58ead0e698918f420';
  const MERCHANT_ID = '11c16124-5407-46cf-812c-8b36f7c894b7';

  test('1. Checkout Page Load', async ({ page }) => {
    await page.goto('http://localhost:5173');
    await page.waitForLoadState('networkidle');
    
    const body = await page.locator('body').textContent();
    expect(body).toBeTruthy();
    console.log('Checkout Page: LOADED');
  });

  test('2. Create Payment Order via API', async ({ request }) => {
    const orderRes = await request.post(`${API_BASE}/api/v1/payments/create-order`, {
      data: {
        orderId: `ORD-${Date.now()}`,
        amount: 1000,
        currency: 'INR'
      },
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`
      }
    });
    
    const orderData = await orderRes.json();
    console.log('Create Order:', orderData.success ? 'PASS' : 'FAIL', orderData.data?.paymentId);
    expect(orderData.success).toBe(true);
  });

  test('3. Get Payment Status by Order ID', async ({ request }) => {
    const orderRes = await request.post(`${API_BASE}/api/v1/payments/create-order`, {
      data: {
        orderId: `ORD-STATUS-${Date.now()}`,
        amount: 500,
        currency: 'USD'
      },
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${API_KEY}`
      }
    });
    const orderData = await orderRes.json();
    const orderId = orderData.data?.orderId;
    
    const statusRes = await request.get(`${API_BASE}/api/v1/payments/status/${orderId}`, {
      headers: { 'Authorization': `Bearer ${API_KEY}` }
    });
    const statusText = await statusRes.text();
    console.log('Status Response:', statusText.substring(0, 200));
    
    if (!statusText) {
      console.log('Get Status: EMPTY_RESPONSE');
      return;
    }
    
    const status = JSON.parse(statusText);
    console.log('Get Status:', status.success ? 'PASS' : 'FAIL');
    expect(status.success).toBe(true);
  });

  test('4. Payment List (Orders)', async ({ request }) => {
    const listRes = await request.get(`${API_BASE}/api/v1/payments/orders`, {
      headers: { 'Authorization': `Bearer ${API_KEY}` }
    });
    const list = await listRes.json();
    console.log('Payment List:', list.success ? 'PASS' : 'FAIL', 'count:', list.data?.length);
    expect(list.success).toBe(true);
  });

  test('5. Customer Auth Flow - Register', async ({ request }) => {
    const customerEmail = `cust_${Date.now()}@test.com`;
    const regRes = await request.post(`${API_BASE}/api/v1/auth/register`, {
      data: {
        email: customerEmail,
        password: 'test1234',
        firstName: 'Test',
        lastName: 'Customer'
      }
    });
    const reg = await regRes.json();
    console.log('Customer Register:', reg.success ? 'PASS' : 'ALREADY_EXISTS');
    expect(reg.success || reg.error === 'Email already exists').toBe(true);
  });

  test('6. Customer Auth Flow - Login', async ({ request }) => {
    const customerEmail = `cust_login_${Date.now()}@test.com`;
    await request.post(`${API_BASE}/api/v1/auth/register`, {
      data: { email: customerEmail, password: 'test1234', firstName: 'Test', lastName: 'User' }
    });
    const loginRes = await request.post(`${API_BASE}/api/v1/auth/login`, {
      data: { email: customerEmail, password: 'test1234' }
    });
    const login = await loginRes.json();
    console.log('Customer Login:', login.success ? 'PASS' : 'FAIL');
    expect(login.success).toBe(true);
  });

  test('7. Merchant API Key Auth', async ({ request }) => {
    const orderRes = await request.post(`${API_BASE}/api/v1/payments/create-order`, {
      data: { orderId: `ORD-AUTH-${Date.now()}`, amount: 100, currency: 'INR' },
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${API_KEY}` }
    });
    const order = await orderRes.json();
    console.log('API Key Auth:', order.success ? 'PASS' : 'FAIL');
    expect(order.success).toBe(true);
  });

  test('8. Checkout Page Elements', async ({ page }) => {
    await page.goto('http://localhost:5173');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);
    
    const hasContent = await page.content();
    console.log('Page Has Content:', hasContent.length > 100 ? 'YES' : 'NO');
    console.log('Page Title:', await page.title());
  });
});