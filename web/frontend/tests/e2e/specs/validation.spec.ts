import { test, expect } from '@playwright/test';
import { CheckoutPage } from '../pages';

test.describe('Payment Validation', () => {
  test.describe('Card Number Validation', () => {
    test('should reject card number with less than 16 digits', async ({ page }) => {
      await page.goto('/');
      const cardNumberInput = page.locator('input[placeholder*="4242"]');
      
      await cardNumberInput.fill('4242 4242 4242');
      
      await expect(page.locator('text=Enter a valid 16-digit card number')).toBeVisible();
    });

    test('should reject card number failing Luhn check', async ({ page }) => {
      await page.goto('/');
      const cardNumberInput = page.locator('input[placeholder*="4242"]');
      
      await cardNumberInput.fill('4242 4242 4242 4243');
      
      await expect(page.locator('text=Enter a valid 16-digit card number')).toBeVisible();
    });

    test('should accept valid Visa test card', async ({ page }) => {
      await page.goto('/');
      const cardNumberInput = page.locator('input[placeholder*="4242"]');
      
      await cardNumberInput.fill('4242 4242 4242 4242');
      
      await expect(page.locator('text=✓ Card number is valid')).toBeVisible();
    });

    test('should accept valid Mastercard test card', async ({ page }) => {
      await page.goto('/');
      const cardNumberInput = page.locator('input[placeholder*="4242"]');
      
      await cardNumberInput.fill('5555 5555 5555 4444');
      
      await expect(page.locator('text=✓ Card number is valid')).toBeVisible();
    });
  });

  test.describe('Expiry Date Validation', () => {
    test('should accept valid future expiry', async ({ page }) => {
      await page.goto('/');
      const expiryInput = page.locator('input[placeholder="MM/YY"]');
      
      await expiryInput.fill('12/28');
      
      await expect(page.locator('input[placeholder="MM/YY"]')).toHaveValue('12/28');
    });

    test('should reject past expiry', async ({ page }) => {
      await page.goto('/');
      const expiryInput = page.locator('input[placeholder="MM/YY"]');
      
      await expiryInput.fill('01/20');
      await page.locator('button:has-text("Pay")').first().click();
      
      await expect(page.locator('text=Use MM/YY format')).toBeVisible();
    });

    test('should reject invalid month', async ({ page }) => {
      await page.goto('/');
      const expiryInput = page.locator('input[placeholder="MM/YY"]');
      
      await expiryInput.fill('13/28');
      await page.locator('button:has-text("Pay")').first().click();
      
      await expect(page.locator('text=Use MM/YY format')).toBeVisible();
    });

    test('should reject invalid format', async ({ page }) => {
      await page.goto('/');
      const expiryInput = page.locator('input[placeholder="MM/YY"]');
      
      await expiryInput.fill('12-28');
      await page.locator('button:has-text("Pay")').first().click();
      
      await expect(page.locator('text=Use MM/YY format')).toBeVisible();
    });
  });

  test.describe('CVV Validation', () => {
    test('should accept 3-digit CVV', async ({ page }) => {
      await page.goto('/');
      const cvvInput = page.locator('input[type="password"]');
      
      await cvvInput.fill('123');
      
      await expect(cvvInput).toHaveValue('123');
    });

    test('should reject 2-digit CVV', async ({ page }) => {
      await page.goto('/');
      const cvvInput = page.locator('input[type="password"]');
      
      await cvvInput.fill('12');
      await page.locator('button:has-text("Pay")').first().click();
      
      await expect(page.locator('text=CVV must be 3 digits')).toBeVisible();
    });

    test('should reject 4-digit CVV', async ({ page }) => {
      await page.goto('/');
      const cvvInput = page.locator('input[type="password"]');
      
      await cvvInput.fill('1234');
      
      await expect(cvvInput).toHaveValue('123');
    });

    test('should mask CVV input', async ({ page }) => {
      await page.goto('/');
      const cvvInput = page.locator('input[type="password"]');
      
      await expect(cvvInput).toHaveAttribute('type', 'password');
    });
  });

  test.describe('Cardholder Name Validation', () => {
    test('should accept valid name', async ({ page }) => {
      await page.goto('/');
      const nameInput = page.locator('input[placeholder="Jordan Lee"]');
      
      await nameInput.fill('John Smith');
      
      await expect(nameInput).toHaveValue('John Smith');
    });

    test('should reject empty name', async ({ page }) => {
      await page.goto('/');
      const nameInput = page.locator('input[placeholder="Jordan Lee"]');
      
      await nameInput.fill('');
      await page.locator('button:has-text("Pay")').first().click();
      
      await expect(page.locator('text=Cardholder name is required')).toBeVisible();
    });

    test('should reject whitespace-only name', async ({ page }) => {
      await page.goto('/');
      const nameInput = page.locator('input[placeholder="Jordan Lee"]');
      
      await nameInput.fill('   ');
      await page.locator('button:has-text("Pay")').first().click();
      
      await expect(page.locator('text=Cardholder name is required')).toBeVisible();
    });
  });
});

test.describe('Error Handling', () => {
  test('should show error when API is unreachable', async ({ page }) => {
    await page.route('**/api/**', route => route.abort());
    await page.goto('/');
    
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    const expiryInput = page.locator('input[placeholder="MM/YY"]');
    const cvvInput = page.locator('input[type="password"]');
    const nameInput = page.locator('input[placeholder="Jordan Lee"]');
    const payButton = page.locator('button:has-text("Pay")').first();

    await cardNumberInput.fill('4242 4242 4242 4242');
    await expiryInput.fill('12/28');
    await cvvInput.fill('123');
    await nameInput.fill('Test User');
    await payButton.click();

    await expect(page.locator('.text-red-700')).toBeVisible({ timeout: 10000 });
  });

  test('should clear error when user starts typing', async ({ page }) => {
    await page.goto('/');
    
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    await cardNumberInput.fill('1234');
    await page.locator('button:has-text("Pay")').first().click();
    
    await expect(page.locator('text=Enter a valid 16-digit card number')).toBeVisible();
    
    await cardNumberInput.fill('4242 4242 4242 4242');
    
    await expect(page.locator('text=Enter a valid 16-digit card number')).not.toBeVisible();
  });
});

test.describe('Accessibility', () => {
  test('should have proper ARIA labels on inputs', async ({ page }) => {
    await page.goto('/');
    
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    await expect(cardNumberInput).toBeVisible();
  });

  test('should be navigable with keyboard', async ({ page }) => {
    await page.goto('/');
    
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    
    const focusedElement = await page.evaluate(() => document.activeElement?.tagName);
    expect(focusedElement).not.toBeNull();
  });

  test('should show focus indicators', async ({ page }) => {
    await page.goto('/');
    
    await page.locator('input[placeholder*="4242"]').focus();
    
    const hasFocusStyle = await page.evaluate(() => {
      const input = document.querySelector('input');
      return window.getComputedStyle(input).outline !== 'none' || 
             window.getComputedStyle(input).boxShadow !== 'none';
    });
    expect(hasFocusStyle).toBeTruthy();
  });
});

test.describe('Performance', () => {
  test('should load within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    const loadTime = Date.now() - startTime;
    
    expect(loadTime).toBeLessThan(2000);
  });

  test('should respond to input quickly', async ({ page }) => {
    await page.goto('/');
    
    const cardNumberInput = page.locator('input[placeholder*="4242"]');
    const startTime = Date.now();
    await cardNumberInput.fill('4242');
    const inputTime = Date.now() - startTime;
    
    expect(inputTime).toBeLessThan(100);
  });
});
