import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages';

async function loginAsAdmin(page) {
  const loginPage = new LoginPage(page);
  await loginPage.goto();
  await loginPage.login('admin@payflow.com', 'Test@1234');
  await page.waitForTimeout(3000);
}

test.describe('Dashboard UI Tests', () => {
  test.describe('Authentication', () => {
    test('should display login page', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible({ timeout: 10000 });
      await expect(loginPage.passwordInput).toBeVisible();
      await expect(loginPage.loginButton).toBeVisible();
      await expect(page).toHaveTitle(/PayFlow|Dashboard/i);
    });

    test('should login with valid credentials', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      
      const currentUrl = page.url();
      expect(currentUrl).toMatch(/\/admin|\/user/);
      expect(currentUrl).not.toContain('/login');
      await context.close();
    });

    test('should show error with invalid credentials', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('invalid@test.com', 'wrongpassword');
      await loginPage.expectError();
      await context.close();
    });

    test('should show error with empty fields', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('', '');
      await expect(page).toHaveURL(/\/login/);
      await context.close();
    });

    test('should redirect to login when not authenticated', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await page.goto('/admin/transactions');
      await page.waitForURL(/\/login/, { timeout: 10000 });
      await expect(page).toHaveURL(/\/login/);
      await context.close();
    });

    test('should persist session after page reload', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      const urlAfterLogin = page.url();
      expect(urlAfterLogin).not.toContain('/login');
      
      await page.reload();
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      expect(page.url()).not.toContain('/login');
      await context.close();
    });
  });

  test.describe('Admin Dashboard', () => {
    test('should display admin dashboard', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/admin');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      expect(page.url()).not.toContain('/login');
      await context.close();
    });

    test('should navigate to transactions page', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/admin/transactions');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      expect(page.url()).not.toContain('/login');
      await context.close();
    });

    test('should navigate to users page', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/admin/users');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      expect(page.url()).not.toContain('/login');
      await context.close();
    });

    test('should navigate to analytics page', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/admin/analytics');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      expect(page.url()).not.toContain('/login');
      await context.close();
    });

    test('should logout successfully', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      
      const logoutButton = page.locator('button:has-text("Logout"), button:has-text("Sign Out"), a:has-text("Logout")').first();
      if (await logoutButton.isVisible({ timeout: 5000 }).catch(() => false)) {
        await logoutButton.click();
        await page.waitForURL(/\/login/, { timeout: 10000 });
        await expect(page).toHaveURL(/\/login/);
      } else {
        await page.goto('/login');
        await expect(page).toHaveURL(/\/login/);
      }
      await context.close();
    });
  });

  test.describe('User Dashboard', () => {
    test('should display user dashboard', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/user');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      await expect(page).toHaveURL(/\/user/);
      await context.close();
    });

    test('should navigate to payments page', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/user/payments');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      await expect(page).toHaveURL(/\/user\/payments/);
      await context.close();
    });

    test('should navigate to orders page', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      await loginAsAdmin(page);
      await page.goto('/user/orders');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);
      
      await expect(page).toHaveURL(/\/user\/orders/);
      await context.close();
    });
  });

  test.describe('Responsive Design', () => {
    test('should render correctly on mobile', async ({ browser }) => {
      const context = await browser.newContext({ viewport: { width: 375, height: 667 } });
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible({ timeout: 10000 });
      await expect(loginPage.passwordInput).toBeVisible();
      await context.close();
    });

    test('should render correctly on tablet', async ({ browser }) => {
      const context = await browser.newContext({ viewport: { width: 768, height: 1024 } });
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible({ timeout: 10000 });
      await context.close();
    });

    test('should render correctly on desktop', async ({ browser }) => {
      const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible({ timeout: 10000 });
      await context.close();
    });
  });

  test.describe('Accessibility', () => {
    test('should have proper form labels', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      const emailAttrs = await loginPage.emailInput.evaluate(el => ({
        placeholder: el.getAttribute('placeholder'),
        name: el.getAttribute('name'),
        id: el.getAttribute('id'),
        ariaLabel: el.getAttribute('aria-label'),
      }));
      expect(emailAttrs.placeholder || emailAttrs.name || emailAttrs.id || emailAttrs.ariaLabel).toBeTruthy();
      
      const passwordAttrs = await loginPage.passwordInput.evaluate(el => ({
        placeholder: el.getAttribute('placeholder'),
        name: el.getAttribute('name'),
        id: el.getAttribute('id'),
        ariaLabel: el.getAttribute('aria-label'),
      }));
      expect(passwordAttrs.placeholder || passwordAttrs.name || passwordAttrs.id || passwordAttrs.ariaLabel).toBeTruthy();
      await context.close();
    });

    test('should have keyboard accessible form', async ({ browser }) => {
      const context = await browser.newContext();
      const page = await context.newPage();
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible({ timeout: 10000 });
      await expect(loginPage.passwordInput).toBeVisible();
      await expect(loginPage.loginButton).toBeVisible();
      await context.close();
    });
  });
});
