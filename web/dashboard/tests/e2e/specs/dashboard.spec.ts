import { test, expect } from '@playwright/test';
import { LoginPage, DashboardPage, TransactionsPage, UserDashboardPage } from '../pages';

test.describe('Dashboard UI Tests', () => {
  test.describe('Authentication', () => {
    test('should display login page', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible();
      await expect(loginPage.passwordInput).toBeVisible();
      await expect(loginPage.loginButton).toBeVisible();
      await expect(page).toHaveTitle(/PayFlow|Dashboard|Admin/i);
    });

    test('should login with valid credentials', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('admin@payflow.com', 'Test@1234');
      await loginPage.expectSuccess();
      
      await expect(page).toHaveURL(/\/admin|\/dashboard/);
    });

    test('should show error with invalid credentials', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('invalid@test.com', 'wrongpassword');
      await loginPage.expectError();
    });

    test('should show error with empty fields', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('', '');
      await expect(loginPage.loginButton).toBeDisabled().or(expect(loginPage.errorMessage).toBeVisible());
    });

    test('should redirect to login when accessing protected route', async ({ page }) => {
      await page.goto('/admin/transactions');
      await page.waitForURL(/\/login/, { timeout: 10000 });
      await expect(page).toHaveURL(/\/login/);
    });

    test('should persist session after page reload', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('admin@payflow.com', 'Test@1234');
      await loginPage.expectSuccess();
      
      await page.reload();
      await expect(page).not.toHaveURL(/\/login/);
    });
  });

  test.describe('Admin Dashboard', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('admin@payflow.com', 'Test@1234');
      await loginPage.expectSuccess();
    });

    test('should display admin dashboard', async ({ page }) => {
      const dashboard = new DashboardPage(page);
      await dashboard.goto();
      
      await expect(dashboard.navMenu).toBeVisible();
      await expect(dashboard.userMenu).toBeVisible();
    });

    test('should navigate to transactions page', async ({ page }) => {
      const dashboard = new DashboardPage(page);
      await dashboard.navigateToTransactions();
      
      const transactionsPage = new TransactionsPage(page);
      await transactionsPage.expectTableVisible();
    });

    test('should navigate to users page', async ({ page }) => {
      const dashboard = new DashboardPage(page);
      await dashboard.navigateToUsers();
      
      await expect(page).toHaveURL(/\/admin\/users/);
    });

    test('should navigate to analytics page', async ({ page }) => {
      const dashboard = new DashboardPage(page);
      await dashboard.navigateToAnalytics();
      
      await expect(page).toHaveURL(/\/admin\/analytics/);
    });

    test('should logout successfully', async ({ page }) => {
      const dashboard = new DashboardPage(page);
      await dashboard.logout();
      
      await expect(page).toHaveURL(/\/login/);
    });
  });

  test.describe('User Dashboard', () => {
    test.beforeEach(async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      await loginPage.login('admin@payflow.com', 'Test@1234');
      await loginPage.expectSuccess();
    });

    test('should display user dashboard', async ({ page }) => {
      const userDashboard = new UserDashboardPage(page);
      await userDashboard.goto();
      
      await expect(page).toHaveURL(/\/user\/dashboard|\/user\/payments|\/dashboard/);
    });

    test('should navigate to payments page', async ({ page }) => {
      const userDashboard = new UserDashboardPage(page);
      await userDashboard.navigateToPayments();
      
      await expect(page).toHaveURL(/\/user\/payments/);
    });

    test('should navigate to orders page', async ({ page }) => {
      const userDashboard = new UserDashboardPage(page);
      await userDashboard.navigateToOrders();
      
      await expect(page).toHaveURL(/\/user\/orders/);
    });
  });

  test.describe('Responsive Design', () => {
    test('should render correctly on mobile', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible();
      await expect(loginPage.passwordInput).toBeVisible();
    });

    test('should render correctly on tablet', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible();
    });

    test('should render correctly on desktop', async ({ page }) => {
      await page.setViewportSize({ width: 1920, height: 1080 });
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toBeVisible();
    });
  });

  test.describe('Accessibility', () => {
    test('should have proper form labels', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await expect(loginPage.emailInput).toHaveAttribute('aria-label').or(expect(loginPage.emailInput).toHaveAttribute('placeholder', /email/i));
      await expect(loginPage.passwordInput).toHaveAttribute('aria-label').or(expect(loginPage.passwordInput).toHaveAttribute('placeholder', /password/i));
    });

    test('should be keyboard navigable', async ({ page }) => {
      const loginPage = new LoginPage(page);
      await loginPage.goto();
      
      await page.keyboard.press('Tab');
      await expect(loginPage.emailInput).toBeFocused();
      
      await page.keyboard.press('Tab');
      await expect(loginPage.passwordInput).toBeFocused();
      
      await page.keyboard.press('Tab');
      await expect(loginPage.loginButton).toBeFocused();
    });
  });
});
