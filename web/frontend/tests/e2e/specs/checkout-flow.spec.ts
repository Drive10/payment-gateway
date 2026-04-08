import { test, expect } from '@playwright/test';

test.describe('Checkout Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should load checkout page with all elements', async ({ page }) => {
    await expect(page.locator('h1:has-text("PayFlow")')).toBeVisible();
    await expect(page.locator('h2:has-text("Checkout")')).toBeVisible();
    await expect(page.locator('text=Secure Connection')).toBeVisible();
  });

  test('should display order summary section', async ({ page }) => {
    await expect(page.locator('text=Order Summary')).toBeVisible();
    await expect(page.locator('text=Total Amount')).toBeVisible();
    await expect(page.locator('text=Amount')).toBeVisible();
  });

  test('should display payment method options', async ({ page }) => {
    await expect(page.locator('button:has-text("Credit/Debit Card")')).toBeVisible();
    await expect(page.locator('button:has-text("UPI")')).toBeVisible();
  });

  test('should switch between payment methods', async ({ page }) => {
    const cardButton = page.locator('button:has-text("Credit/Debit Card")');
    const upiButton = page.locator('button:has-text("UPI")');
    
    await upiButton.click();
    await expect(upiButton).toHaveClass(/border-cyan-500/);
    
    await cardButton.click();
    await expect(cardButton).toHaveClass(/border-cyan-500/);
  });

  test('should display card form when card method selected', async ({ page }) => {
    await expect(page.locator('input[placeholder*="4242"]')).toBeVisible();
    await expect(page.locator('input[placeholder="MM/YY"]')).toBeVisible();
    await expect(page.locator('input[type="password"][placeholder="•••"]')).toBeVisible();
    await expect(page.locator('input[placeholder="Jordan Lee"]')).toBeVisible();
  });

  test('should display UPI QR section when UPI selected', async ({ page }) => {
    await page.locator('button:has-text("UPI")').click();
    await expect(page.locator('text=UPI ID')).toBeVisible();
  });

  test('should disable pay button when card form incomplete', async ({ page }) => {
    const payButton = page.locator('button:has-text("Pay")').first();
    await expect(payButton).toBeDisabled();
  });

  test('should enable pay button when card form is complete', async ({ page }) => {
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    const expiryInput = page.locator('input[placeholder="MM/YY"]');
    const cvvInput = page.locator('input[type="password"]');
    const nameInput = page.locator('input[placeholder="Jordan Lee"]');
    const payButton = page.locator('button:has-text("Pay")').first();

    await cardNumberInput.fill('4242 4242 4242 4242');
    await expiryInput.fill('12/28');
    await cvvInput.fill('123');
    await nameInput.fill('Test User');

    await expect(payButton).toBeEnabled();
  });

  test('should show validation errors for invalid card number', async ({ page }) => {
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    
    await cardNumberInput.fill('1234 5678 9012 3456');
    await page.locator('button:has-text("Pay")').first().click();
    
    await expect(page.locator('text=Enter a valid 16-digit card number')).toBeVisible();
  });

  test('should show validation errors for invalid expiry', async ({ page }) => {
    const expiryInput = page.locator('input[placeholder="MM/YY"]');
    
    await expiryInput.fill('13/28');
    await page.locator('button:has-text("Pay")').first().click();
    
    await expect(page.locator('text=Use MM/YY format')).toBeVisible();
  });

  test('should show validation errors for invalid CVV', async ({ page }) => {
    const cvvInput = page.locator('input[type="password"]');
    
    await cvvInput.fill('12');
    await page.locator('button:has-text("Pay")').first().click();
    
    await expect(page.locator('text=CVV must be 3 digits')).toBeVisible();
  });

  test('should show validation errors for empty cardholder name', async ({ page }) => {
    const nameInput = page.locator('input[placeholder="Jordan Lee"]');
    
    await nameInput.fill('   ');
    await page.locator('button:has-text("Pay")').first().click();
    
    await expect(page.locator('text=Cardholder name is required')).toBeVisible();
  });

  test('should show transaction mode options', async ({ page }) => {
    await expect(page.locator('button:has-text("Production")')).toBeVisible();
    await expect(page.locator('button:has-text("Test/Sandbox")')).toBeVisible();
  });

  test('should toggle transaction mode', async ({ page }) => {
    const testButton = page.locator('button:has-text("Test/Sandbox")');
    
    await testButton.click();
    await expect(testButton).toHaveClass(/border-cyan-500/);
  });

  test('should display security badges', async ({ page }) => {
    await expect(page.locator('text=SSL Encrypted')).toBeVisible();
    await expect(page.locator('text=PCI Compliant')).toBeVisible();
  });

  test('should display processing fee as free', async ({ page }) => {
    await expect(page.locator('text=Processing Fee')).toBeVisible();
    await expect(page.locator('text=Free').first()).toBeVisible();
  });

  test('should display merchant info', async ({ page }) => {
    await expect(page.locator('text=Nova Commerce')).toBeVisible();
  });

  test('should display payment note', async ({ page }) => {
    await expect(page.locator('text=Secure checkout for a production-style fintech payment flow')).toBeVisible();
  });
});

test.describe('Processing Page', () => {
  test('should show loading spinner', async ({ page }) => {
    await page.goto('/processing');
    await expect(page.locator('.animate-spin')).toBeVisible();
  });
});

test.describe('Payment Card Brand Detection', () => {
  test('should show Visa badge for Visa cards', async ({ page }) => {
    await page.goto('/');
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    await cardNumberInput.fill('4111 1111 1111 1111');
    await expect(page.locator('text=Visa').first()).toBeVisible();
  });

  test('should show Mastercard badge for Mastercard', async ({ page }) => {
    await page.goto('/');
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    await cardNumberInput.fill('5111 1111 1111 1111');
    await expect(page.locator('text=Mastercard').first()).toBeVisible();
  });
});
