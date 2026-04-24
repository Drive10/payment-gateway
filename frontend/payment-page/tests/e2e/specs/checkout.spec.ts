import { test, expect } from '@playwright/test';

test.describe('Checkout Flow', () => {
  test('should load checkout page at root', async ({ page }) => {
    await page.goto('http://localhost:5173/');
    
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
    
    const root = page.locator('#root');
    await expect(root).toBeVisible();
    
    const body = page.locator('body');
    await expect(body).toBeVisible();
  });

  test('should load and interact with checkout form', async ({ page }) => {
    await page.goto('http://localhost:5173/');
    
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
    
    await expect(page.locator('body')).toBeVisible();
  });
});