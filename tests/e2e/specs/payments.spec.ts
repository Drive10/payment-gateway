import { test, expect } from '../fixtures/test-fixtures';
import { LoginPage } from '../pages/LoginPage';

test.describe('User Payments Page', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'Test@1234');
    await page.waitForURL(/\/admin/, { timeout: 10000 });
    await page.goto('/user/payments');
    await page.waitForLoadState('networkidle');
  });

  test('should load payments page', async ({ page }) => {
    await expect(page.locator('main h1').first()).toBeVisible({ timeout: 10000 });
  });

  test('should display content', async ({ page }) => {
    await expect(page.locator('main')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Admin Transactions Page', () => {
  test.beforeEach(async ({ page }) => {
    page.on('console', msg => console.log('Console:', msg.type(), msg.text()));
    
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'Test@1234');
    await page.waitForURL(/\/admin/, { timeout: 10000 });
    await page.goto('/admin/transactions');
    await page.waitForLoadState('networkidle');
  });

  test('should load transactions page', async ({ page }) => {
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Payment Flow', () => {
  test('login and navigate to payments works', async ({ page, loginPage }) => {
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'Test@1234');
    await page.waitForURL(/\/admin/);
    await page.goto('/user/payments');
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible({ timeout: 10000 });
  });
});
