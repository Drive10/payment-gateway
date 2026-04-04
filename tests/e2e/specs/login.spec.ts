import { test, expect } from '../fixtures/test-fixtures';

test.describe('Login Page', () => {
  test('shows login form', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.expectVisible();
  });

  test('displays demo credentials', async ({ loginPage }) => {
    await loginPage.goto();
    await expect(loginPage.demoCredentials).toBeVisible();
  });

  test('login as admin succeeds', async ({ loginPage, page }) => {
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'Test@1234');
    await page.waitForURL(/\/admin/);
  });

  test('login with wrong password fails', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.login('admin@payflow.com', 'wrongpassword');
    await loginPage.expectError('Invalid');
  });

  test('login with non-existent email fails', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.login('nobody@example.com', 'Test@1234');
    await loginPage.expectError('Invalid');
  });

  test('empty form submission shows validation', async ({ loginPage }) => {
    await loginPage.goto();
    await loginPage.submitButton.click();
    await expect(loginPage.emailInput).toBeVisible();
  });
});
