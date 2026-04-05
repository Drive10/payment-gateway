/**
 * E2E Tests using Playwright
 * Tests complete user flows from frontend to backend
 */

const { test, expect } = require('@playwright/test');

test.describe('Payment Gateway E2E Tests', () => {
  
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000');
  });

  // ============ AUTHENTICATION FLOWS ============

  test.describe('Authentication Flow', () => {
    test('should register new user successfully', async ({ page }) => {
      // Navigate to register page
      await page.click('text=Register');
      
      // Fill registration form
      await page.fill('input[name="email"]', `testuser_${Date.now()}@payflow.com`);
      await page.fill('input[name="password"]', 'Test@1234');
      await page.fill('input[name="firstName"]', 'Test');
      await page.fill('input[name="lastName"]', 'User');
      
      // Submit form
      await page.click('button[type="submit"]');
      
      // Wait for redirect to dashboard
      await page.waitForURL('**/dashboard');
      
      // Verify user is logged in
      await expect(page.locator('text=Welcome')).toBeVisible();
    });

    test('should login existing user', async ({ page }) => {
      // Fill login form
      await page.fill('input[name="email"]', 'admin@payflow.com');
      await page.fill('input[name="password"]', 'Admin@1234');
      
      // Submit
      await page.click('button[type="submit"]');
      
      // Verify redirect
      await page.waitForURL('**/dashboard');
      await expect(page.locator('text=Dashboard')).toBeVisible();
    });

    test('should show error for invalid credentials', async ({ page }) => {
      // Fill with wrong credentials
      await page.fill('input[name="email"]', 'wrong@payflow.com');
      await page.fill('input[name="password"]', 'wrongpass');
      
      // Submit
      await page.click('button[type="submit"]');
      
      // Verify error message
      await expect(page.locator('text=Invalid credentials')).toBeVisible();
    });
  });

  // ============ PAYMENT FLOWS ============

  test.describe('Payment Flow', () => {
    let authToken;

    test.beforeEach(async ({ page }) => {
      // Login first
      await page.fill('input[name="email"]', 'admin@payflow.com');
      await page.fill('input[name="password"]', 'Admin@1234');
      await page.click('button[type="submit"]');
      await page.waitForURL('**/dashboard');
      
      // Get auth token from localStorage
      authToken = await page.evaluate(() => localStorage.getItem('authToken'));
    });

    test('should create new payment', async ({ page }) => {
      // Navigate to payments
      await page.click('text=Payments');
      await page.waitForURL('**/payments');
      
      // Click create payment
      await page.click('text=New Payment');
      
      // Fill payment form
      await page.selectOption('select[name="paymentMethod"]', 'CARD');
      await page.fill('input[name="amount"]', '1000');
      await page.fill('input[name="orderId"]', `ORDER-${Date.now()}`);
      
      // Submit
      await page.click('button[type="submit"]');
      
      // Verify success message
      await expect(page.locator('text=Payment created successfully')).toBeVisible();
      
      // Verify payment appears in list
      await expect(page.locator('text=ORDER-')).toBeVisible();
    });

    test('should process refund', async ({ page }) => {
      // Navigate to payment details
      await page.click('text=Payments');
      await page.waitForURL('**/payments');
      
      // Click on a completed payment
      await page.click('text=COMPLETED >> nth=0');
      
      // Click refund button
      await page.click('text=Refund');
      
      // Fill refund form
      await page.fill('input[name="reason"]', 'Customer request');
      
      // Confirm
      await page.click('text=Confirm Refund');
      
      // Verify status updated
      await expect(page.locator('text=REFUNDED')).toBeVisible();
    });

    test('should search payments with filters', async ({ page }) => {
      // Navigate to payments
      await page.click('text=Payments');
      await page.waitForURL('**/payments');
      
      // Use search
      await page.fill('input[placeholder="Search..."]', 'failed payments last 24h');
      await page.click('text=Search');
      
      // Wait for results
      await page.waitForSelector('.payment-list');
      
      // Verify results
      const paymentCount = await page.locator('.payment-item').count();
      expect(paymentCount).toBeGreaterThan(0);
    });
  });

  // ============ GRAPHQL API TESTS ============

  test.describe('GraphQL API Tests', () => {
    test('should execute search query via GraphQL', async ({ request }) => {
      const response = await request.post('http://localhost:8087/graphql', {
        data: {
          query: `
            query {
              searchPayments(query: "failed payments last 24h") {
                payments {
                  id
                  amount
                  status
                }
                totalCount
              }
            }
          `
        }
      });

      expect(response.ok()).toBeTruthy();
      const body = await response.json();
      expect(body.data.searchPayments).toBeDefined();
      expect(body.data.searchPayments.payments).toBeInstanceOf(Array);
    });

    test('should create payment via GraphQL mutation', async ({ request }) => {
      const response = await request.post('http://localhost:8087/graphql', {
        data: {
          query: `
            mutation {
              createPayment(input: {
                orderId: "order-graphql-test"
                amount: 500
                currency: "INR"
                paymentMethod: CARD
              }) {
                id
                status
              }
            }
          `
        }
      });

      expect(response.ok()).toBeTruthy();
      const body = await response.json();
      expect(body.data.createPayment.id).toBeDefined();
      expect(body.data.createPayment.status).toBe('PENDING');
    });

    test('should fetch analytics via GraphQL', async ({ request }) => {
      const response = await request.post('http://localhost:8087/graphql', {
        data: {
          query: `
            query {
              analyticsSummary {
                totalTransactions
                successfulTransactions
                successRate
              }
              dailyMetrics(days: 7) {
                date
                totalTransactions
                totalVolume
              }
            }
          `
        }
      });

      expect(response.ok()).toBeTruthy();
      const body = await response.json();
      expect(body.data.analyticsSummary.totalTransactions).toBeGreaterThan(0);
      expect(body.data.dailyMetrics).toHaveLength(7);
    });
  });

  // ============ OBSERVABILITY TESTS ============

  test.describe('Observability Tests', () => {
    test('should have health endpoint', async ({ request }) => {
      const response = await request.get('http://localhost:8080/actuator/health');
      expect(response.ok()).toBeTruthy();
      const body = await response.json();
      expect(body.status).toBe('UP');
    });

    test('should have prometheus metrics', async ({ request }) => {
      const response = await request.get('http://localhost:8080/actuator/prometheus');
      expect(response.ok()).toBeTruthy();
      const body = await response.text();
      expect(body).toContain('http_server_requests_seconds');
    });
  });

  // ============ SECURITY TESTS ============

  test.describe('Security Tests', () => {
    test('should reject unauthenticated requests', async ({ request }) => {
      const response = await request.get('http://localhost:8080/api/v1/payments');
      expect(response.status()).toBe(401);
    });

    test('should have rate limiting', async ({ request }) => {
      // Make many rapid requests
      for (let i = 0; i < 15; i++) {
        await request.get('http://localhost:8080/api/v1/auth/health');
      }
      
      // Last request should be rate limited
      const response = await request.get('http://localhost:8080/api/v1/auth/health');
      expect([429, 401]).toContain(response.status());
    });
  });
});
